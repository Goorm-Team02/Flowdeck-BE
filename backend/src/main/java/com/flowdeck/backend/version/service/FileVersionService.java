package com.flowdeck.backend.version.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.dto.ProjectFileEventResponse;
import com.flowdeck.backend.file.realtime.ProjectFileBroadcaster;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.transaction.AfterCommitExecutor;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.DiffLineResponse;
import com.flowdeck.backend.version.dto.DiffOperation;
import com.flowdeck.backend.version.dto.FileConflictResponse;
import com.flowdeck.backend.version.dto.FileTimelineResponse;
import com.flowdeck.backend.version.dto.FileTimelineVersionProjection;
import com.flowdeck.backend.version.dto.FileTimelineVersionResponse;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionDetailResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffLimitResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionListResponse;
import com.flowdeck.backend.version.dto.FileVersionResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreRequest;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileVersionService {

  private static final String SYSTEM_ACTOR_NAME = "시스템";
  private static final String UNKNOWN_ACTOR_NAME = "알 수 없음";
  private static final int DEFAULT_TIMELINE_PAGE = 0;
  private static final int DEFAULT_TIMELINE_SIZE = 20;
  private static final int MAX_TIMELINE_SIZE = 100;
  private static final int MAX_DIFF_LINES = 5_000;
  private static final int MAX_DIFF_CHARACTERS = 200_000;

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final PermissionService permissionService;
  private final UserRepository userRepository;
  private final ProjectFileBroadcaster projectFileBroadcaster;
  private final AfterCommitExecutor afterCommitExecutor;

  public FileVersionService(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      PermissionService permissionService,
      UserRepository userRepository,
      ProjectFileBroadcaster projectFileBroadcaster,
      AfterCommitExecutor afterCommitExecutor) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
    this.projectFileBroadcaster = projectFileBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
  }

  @Transactional(readOnly = true)
  public FileVersionListResponse getVersions(String projectId, Long userId, Long fileId) {
    permissionService.validateProjectReadAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);

    List<FileVersionResponse> versions =
        fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file).stream()
            .map(FileVersionResponse::from)
            .toList();

    return FileVersionListResponse.from(versions);
  }

  @Transactional(readOnly = true)
  public FileVersionDetailResponse getVersion(
      String projectId, Long userId, Long fileId, Long versionId) {
    permissionService.validateProjectReadAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    FileVersion version = getFileVersion(file, versionId);

    return FileVersionDetailResponse.from(version);
  }

  @Transactional(readOnly = true)
  public FileTimelineResponse getTimeline(
      String projectId, Long userId, Long fileId, int page, int size) {
    permissionService.validateProjectReadAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    int normalizedPage = normalizeTimelinePage(page);
    int normalizedSize = normalizeTimelineSize(size);
    Page<FileTimelineVersionProjection> versions =
        fileVersionRepository.findTimelineVersionsByFile(
            file, PageRequest.of(normalizedPage, normalizedSize));

    Map<Long, String> creatorNames = creatorNames(versions.getContent());
    List<FileTimelineVersionResponse> timelineVersions =
        versions.getContent().stream()
            .map(
                version ->
                    FileTimelineVersionResponse.from(
                        version, resolveCreatorName(version.getUserId(), creatorNames)))
            .toList();

    return FileTimelineResponse.from(
        file,
        versions.getTotalElements(),
        normalizedPage,
        normalizedSize,
        versions.hasNext(),
        timelineVersions);
  }

  @Transactional
  public FileVersionCreateResponse createVersion(
      String projectId, Long userId, Long fileId, FileVersionCreateRequest request) {
    permissionService.validateEditor(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    file.increaseVersion();

    FileVersion version =
        new FileVersion(
            file,
            userId,
            file.getCurrentVersion(),
            file.getCurrentContent(),
            request.getChangeMessage());
    fileVersionRepository.save(version);

    return FileVersionCreateResponse.from(file);
  }

  @Transactional
  public FileVersionRestoreResponse restoreVersion(
      String projectId,
      Long userId,
      Long fileId,
      Long versionId,
      FileVersionRestoreRequest request) {
    permissionService.validateEditor(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    FileVersion version = getFileVersion(file, versionId);

    if (file.getEditRevision() != request.getBaseRevision()) {
      throw new BusinessException(
          ErrorCode.FILE_EDIT_CONFLICT, FileConflictResponse.from(file, request.getBaseRevision()));
    }

    file.updateContent(version.getContent());
    file.increaseEditRevision();
    file.increaseVersion();

    FileVersion restoredVersion =
        new FileVersion(
            file,
            userId,
            file.getCurrentVersion(),
            version.getContent(),
            "버전 " + version.getVersionNumber() + " 복원");

    fileVersionRepository.save(restoredVersion);
    ProjectFileEventResponse event =
        ProjectFileEventResponse.restored(projectId, file, userId, getActorName(userId));

    afterCommitExecutor.run(() -> projectFileBroadcaster.broadcast(event));

    return FileVersionRestoreResponse.from(file);
  }

  @Transactional(readOnly = true)
  public FileVersionDiffResponse getDiff(
      String projectId, Long userId, Long fileId, int fromVersion, int toVersion) {
    permissionService.validateProjectReadAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);

    FileVersion from =
        fileVersionRepository
            .findByFileAndVersionNumber(file, fromVersion)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
    FileVersion to =
        fileVersionRepository
            .findByFileAndVersionNumber(file, toVersion)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));

    validateDiffSize(from, to);
    return createDiffResponse(from, to);
  }

  private ProjectFile getProjectFile(String projectId, Long fileId) {
    Project project =
        projectRepository
            .findByPublicId(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    ProjectFile file =
        projectFileRepository
            .findByIdAndProject(fileId, project)
            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

    if (!file.isFile()) {
      throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
    }

    return file;
  }

  private FileVersion getFileVersion(ProjectFile file, Long versionId) {
    return fileVersionRepository
        .findByFileAndId(file, versionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
  }

  private List<DiffLineResponse> createLineDiff(String oldContent, String newContent) {
    String[] oldLines = oldContent.split("\\R", -1);
    String[] newLines = newContent.split("\\R", -1);

    List<DiffLineResponse> changes = new ArrayList<>();
    List<DiffOperation> operations = createMyersOperations(oldLines, newLines);

    int oldIndex = 0;
    int newIndex = 0;

    for (DiffOperation operation : operations) {
      if ("UNCHANGED".equals(operation.type())) {
        changes.add(
            new DiffLineResponse(
                operation.type(), oldIndex + 1, newIndex + 1, operation.content()));
        oldIndex++;
        newIndex++;
      } else if ("REMOVED".equals(operation.type())) {
        changes.add(
            new DiffLineResponse(operation.type(), oldIndex + 1, null, operation.content()));
        oldIndex++;
      } else {
        changes.add(
            new DiffLineResponse(operation.type(), null, newIndex + 1, operation.content()));
        newIndex++;
      }
    }

    return changes;
  }

  private List<DiffOperation> createMyersOperations(String[] oldLines, String[] newLines) {
    if (oldLines.length == 0 && newLines.length == 0) {
      return List.of();
    }

    int maxDistance = oldLines.length + newLines.length;
    List<Map<Integer, Integer>> trace = new ArrayList<>();
    Map<Integer, Integer> furthestXByDiagonal = new HashMap<>();
    furthestXByDiagonal.put(1, 0);

    for (int distance = 0; distance <= maxDistance; distance++) {
      trace.add(Map.copyOf(furthestXByDiagonal));
      Map<Integer, Integer> nextFurthestXByDiagonal = new HashMap<>();

      for (int diagonal = -distance; diagonal <= distance; diagonal += 2) {
        int x;
        if (diagonal == -distance
            || (diagonal != distance
                && getFurthestX(furthestXByDiagonal, diagonal - 1)
                    < getFurthestX(furthestXByDiagonal, diagonal + 1))) {
          x = getFurthestX(furthestXByDiagonal, diagonal + 1);
        } else {
          x = getFurthestX(furthestXByDiagonal, diagonal - 1) + 1;
        }

        int y = x - diagonal;
        while (x < oldLines.length && y < newLines.length && oldLines[x].equals(newLines[y])) {
          x++;
          y++;
        }

        nextFurthestXByDiagonal.put(diagonal, x);
        if (x >= oldLines.length && y >= newLines.length) {
          return backtrackMyersOperations(trace, oldLines, newLines, distance);
        }
      }

      furthestXByDiagonal = nextFurthestXByDiagonal;
    }

    throw new IllegalStateException("Failed to calculate file version diff.");
  }

  private int getFurthestX(Map<Integer, Integer> furthestXByDiagonal, int diagonal) {
    return furthestXByDiagonal.getOrDefault(diagonal, Integer.MIN_VALUE / 2);
  }

  private List<DiffOperation> backtrackMyersOperations(
      List<Map<Integer, Integer>> trace, String[] oldLines, String[] newLines, int distance) {
    List<DiffOperation> operations = new ArrayList<>();
    int x = oldLines.length;
    int y = newLines.length;

    for (int currentDistance = distance; currentDistance > 0; currentDistance--) {
      Map<Integer, Integer> previousFurthestXByDiagonal = trace.get(currentDistance);
      int diagonal = x - y;
      int previousDiagonal;

      if (diagonal == -currentDistance
          || (diagonal != currentDistance
              && getFurthestX(previousFurthestXByDiagonal, diagonal - 1)
                  < getFurthestX(previousFurthestXByDiagonal, diagonal + 1))) {
        previousDiagonal = diagonal + 1;
      } else {
        previousDiagonal = diagonal - 1;
      }

      int previousX = getFurthestX(previousFurthestXByDiagonal, previousDiagonal);
      int previousY = previousX - previousDiagonal;

      while (x > previousX && y > previousY) {
        operations.add(new DiffOperation("UNCHANGED", oldLines[x - 1]));
        x--;
        y--;
      }

      if (x == previousX) {
        operations.add(new DiffOperation("ADDED", newLines[y - 1]));
        y--;
      } else {
        operations.add(new DiffOperation("REMOVED", oldLines[x - 1]));
        x--;
      }
    }

    while (x > 0 && y > 0) {
      operations.add(new DiffOperation("UNCHANGED", oldLines[x - 1]));
      x--;
      y--;
    }
    while (x > 0) {
      operations.add(new DiffOperation("REMOVED", oldLines[x - 1]));
      x--;
    }
    while (y > 0) {
      operations.add(new DiffOperation("ADDED", newLines[y - 1]));
      y--;
    }

    Collections.reverse(operations);
    return preferRemovedBeforeAdded(operations);
  }

  private FileVersionDiffResponse createDiffResponse(FileVersion from, FileVersion to) {
    List<DiffLineResponse> changes = createLineDiff(from.getContent(), to.getContent());
    int addedLines = countType(changes, "ADDED");
    int removedLines = countType(changes, "REMOVED");

    return new FileVersionDiffResponse(
        from.getVersionNumber(), to.getVersionNumber(), addedLines, removedLines, changes);
  }

  private void validateDiffSize(FileVersion from, FileVersion to) {
    if (exceedsDiffLimit(from.getContent()) || exceedsDiffLimit(to.getContent())) {
      throw new BusinessException(
          ErrorCode.VERSION_DIFF_TOO_LARGE,
          FileVersionDiffLimitResponse.of(
              from.getVersionNumber(),
              to.getVersionNumber(),
              MAX_DIFF_LINES,
              MAX_DIFF_CHARACTERS,
              from.getContent(),
              to.getContent()));
    }
  }

  private boolean exceedsDiffLimit(String content) {
    return content.length() > MAX_DIFF_CHARACTERS || countLines(content) > MAX_DIFF_LINES;
  }

  private int countLines(String content) {
    return content.split("\\R", -1).length;
  }

  private List<DiffOperation> preferRemovedBeforeAdded(List<DiffOperation> operations) {
    List<DiffOperation> orderedOperations = new ArrayList<>(operations);

    for (int index = 0; index < orderedOperations.size() - 1; index++) {
      DiffOperation current = orderedOperations.get(index);
      DiffOperation next = orderedOperations.get(index + 1);
      if ("ADDED".equals(current.type()) && "REMOVED".equals(next.type())) {
        orderedOperations.set(index, next);
        orderedOperations.set(index + 1, current);
        index++;
      }
    }

    return orderedOperations;
  }

  private int countType(List<DiffLineResponse> changes, String type) {
    return (int) changes.stream().filter(change -> type.equals(change.type())).count();
  }

  private int normalizeTimelinePage(int page) {
    return Math.max(page, DEFAULT_TIMELINE_PAGE);
  }

  private int normalizeTimelineSize(int size) {
    if (size < 1) {
      return DEFAULT_TIMELINE_SIZE;
    }

    return Math.min(size, MAX_TIMELINE_SIZE);
  }

  private Map<Long, String> creatorNames(Collection<FileTimelineVersionProjection> versions) {
    List<Long> userIds =
        versions.stream()
            .map(FileTimelineVersionProjection::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    if (userIds.isEmpty()) {
      return Map.of();
    }

    return userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, User::getName));
  }

  private String resolveCreatorName(Long userId, Map<Long, String> creatorNames) {
    if (userId == null) {
      return SYSTEM_ACTOR_NAME;
    }

    return creatorNames.getOrDefault(userId, UNKNOWN_ACTOR_NAME);
  }

  private String getActorName(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    return user.getName();
  }
}
