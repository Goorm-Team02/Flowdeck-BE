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
import com.flowdeck.backend.version.dto.FileConflictResponse;
import com.flowdeck.backend.version.dto.FileTimelineResponse;
import com.flowdeck.backend.version.dto.FileTimelineSelectedVersionResponse;
import com.flowdeck.backend.version.dto.FileTimelineVersionResponse;
import com.flowdeck.backend.version.dto.FileVersionChangeSummary;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionDetailResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionListResponse;
import com.flowdeck.backend.version.dto.FileVersionResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreRequest;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileVersionService {

  private static final String SYSTEM_ACTOR_NAME = "시스템";
  private static final String UNKNOWN_ACTOR_NAME = "알 수 없음";

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
    permissionService.validateProjectAccess(projectId, userId);

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
    permissionService.validateProjectAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    FileVersion version = getFileVersion(file, versionId);

    return FileVersionDetailResponse.from(version);
  }

  @Transactional(readOnly = true)
  public FileTimelineResponse getTimeline(String projectId, Long userId, Long fileId) {
    permissionService.validateProjectAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberAsc(file);

    if (versions.isEmpty()) {
      return FileTimelineResponse.empty(file);
    }

    Map<Long, String> creatorNames = creatorNames(versions);
    Map<Integer, FileVersionChangeSummary> changeSummaries =
        createTimelineChangeSummaries(versions);
    FileVersion selectedVersion = versions.get(versions.size() - 1);
    FileVersionChangeSummary selectedSummary =
        changeSummaries.get(selectedVersion.getVersionNumber());
    FileVersionDiffResponse diffFromPrevious = createDiffFromPrevious(selectedVersion, versions);

    List<FileTimelineVersionResponse> timelineVersions =
        versions.stream()
            .map(
                version -> {
                  FileVersionChangeSummary summary =
                      changeSummaries.get(version.getVersionNumber());
                  return FileTimelineVersionResponse.from(
                      version,
                      resolveCreatorName(version.getUserId(), creatorNames),
                      summary == null ? null : summary.addedLines(),
                      summary == null ? null : summary.removedLines());
                })
            .toList();

    FileTimelineSelectedVersionResponse timelineSelectedVersion =
        FileTimelineSelectedVersionResponse.from(
            selectedVersion,
            resolveCreatorName(selectedVersion.getUserId(), creatorNames),
            selectedSummary == null ? null : selectedSummary.addedLines(),
            selectedSummary == null ? null : selectedSummary.removedLines());

    return FileTimelineResponse.from(
        file, timelineSelectedVersion, diffFromPrevious, timelineVersions);
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
    permissionService.validateProjectAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);

    FileVersion from =
        fileVersionRepository
            .findByFileAndVersionNumber(file, fromVersion)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
    FileVersion to =
        fileVersionRepository
            .findByFileAndVersionNumber(file, toVersion)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));

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

    int[][] lcsLengths = createLcsLengths(oldLines, newLines);
    List<DiffLineResponse> changes = new ArrayList<>();

    int oldIndex = 0;
    int newIndex = 0;
    while (oldIndex < oldLines.length && newIndex < newLines.length) {
      String oldLine = oldLines[oldIndex];
      String newLine = newLines[newIndex];

      if (oldLine.equals(newLine)) {
        changes.add(new DiffLineResponse("UNCHANGED", oldIndex + 1, newIndex + 1, newLine));
        oldIndex++;
        newIndex++;
      } else if (lcsLengths[oldIndex + 1][newIndex] >= lcsLengths[oldIndex][newIndex + 1]) {
        changes.add(new DiffLineResponse("REMOVED", oldIndex + 1, null, oldLine));
        oldIndex++;
      } else {
        changes.add(new DiffLineResponse("ADDED", null, newIndex + 1, newLine));
        newIndex++;
      }
    }

    while (oldIndex < oldLines.length) {
      changes.add(new DiffLineResponse("REMOVED", oldIndex + 1, null, oldLines[oldIndex]));
      oldIndex++;
    }

    while (newIndex < newLines.length) {
      changes.add(new DiffLineResponse("ADDED", null, newIndex + 1, newLines[newIndex]));
      newIndex++;
    }

    return changes;
  }

  private int[][] createLcsLengths(String[] oldLines, String[] newLines) {
    int[][] lengths = new int[oldLines.length + 1][newLines.length + 1];

    for (int oldIndex = oldLines.length - 1; oldIndex >= 0; oldIndex--) {
      for (int newIndex = newLines.length - 1; newIndex >= 0; newIndex--) {
        if (oldLines[oldIndex].equals(newLines[newIndex])) {
          lengths[oldIndex][newIndex] = lengths[oldIndex + 1][newIndex + 1] + 1;
        } else {
          lengths[oldIndex][newIndex] =
              Math.max(lengths[oldIndex + 1][newIndex], lengths[oldIndex][newIndex + 1]);
        }
      }
    }

    return lengths;
  }

  private FileVersionDiffResponse createDiffResponse(FileVersion from, FileVersion to) {
    List<DiffLineResponse> changes = createLineDiff(from.getContent(), to.getContent());
    int addedLines = countType(changes, "ADDED");
    int removedLines = countType(changes, "REMOVED");

    return new FileVersionDiffResponse(
        from.getVersionNumber(), to.getVersionNumber(), addedLines, removedLines, changes);
  }

  private Map<Integer, FileVersionChangeSummary> createTimelineChangeSummaries(
      List<FileVersion> versions) {
    Map<Integer, FileVersionChangeSummary> summaries = new HashMap<>();

    for (int index = 1; index < versions.size(); index++) {
      FileVersion previousVersion = versions.get(index - 1);
      FileVersion currentVersion = versions.get(index);
      summaries.put(
          currentVersion.getVersionNumber(),
          createChangeSummary(previousVersion.getContent(), currentVersion.getContent()));
    }

    return summaries;
  }

  private FileVersionDiffResponse createDiffFromPrevious(
      FileVersion selectedVersion, List<FileVersion> versions) {
    for (int index = 0; index < versions.size(); index++) {
      FileVersion version = versions.get(index);
      if (version.getVersionNumber() != selectedVersion.getVersionNumber()) {
        continue;
      }

      if (index == 0) {
        return null;
      }

      return createDiffResponse(versions.get(index - 1), version);
    }

    throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
  }

  private FileVersionChangeSummary createChangeSummary(String oldContent, String newContent) {
    String[] oldLines = oldContent.split("\\R", -1);
    String[] newLines = newContent.split("\\R", -1);

    int[][] lcsLengths = createLcsLengths(oldLines, newLines);
    int oldIndex = 0;
    int newIndex = 0;
    int addedLines = 0;
    int removedLines = 0;

    while (oldIndex < oldLines.length && newIndex < newLines.length) {
      String oldLine = oldLines[oldIndex];
      String newLine = newLines[newIndex];

      if (oldLine.equals(newLine)) {
        oldIndex++;
        newIndex++;
      } else if (lcsLengths[oldIndex + 1][newIndex] >= lcsLengths[oldIndex][newIndex + 1]) {
        removedLines++;
        oldIndex++;
      } else {
        addedLines++;
        newIndex++;
      }
    }

    removedLines += oldLines.length - oldIndex;
    addedLines += newLines.length - newIndex;

    return new FileVersionChangeSummary(addedLines, removedLines);
  }

  private int countType(List<DiffLineResponse> changes, String type) {
    return (int) changes.stream().filter(change -> type.equals(change.type())).count();
  }

  private Map<Long, String> creatorNames(Collection<FileVersion> versions) {
    List<Long> userIds =
        versions.stream().map(FileVersion::getUserId).filter(Objects::nonNull).distinct().toList();

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
