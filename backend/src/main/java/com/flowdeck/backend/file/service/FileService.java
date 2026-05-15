package com.flowdeck.backend.file.service;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.FlowFile;
import com.flowdeck.backend.file.dto.CreateFileRequest;
import com.flowdeck.backend.file.dto.FileResponse;
import com.flowdeck.backend.file.dto.UpdateFileRequest;
import com.flowdeck.backend.file.exception.DuplicateFileNameException;
import com.flowdeck.backend.file.exception.FileNotFoundException;
import com.flowdeck.backend.file.repository.FlowFileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileService {

  private final FlowFileRepository flowFileRepository;

  public FileService(FlowFileRepository flowFileRepository) {
    this.flowFileRepository = flowFileRepository;
  }

  @Transactional
  public FileResponse createFile(Long projectId, CreateFileRequest request) {
    Long validatedProjectId = requirePositiveId(projectId, "projectId");
    Long validatedParentId = resolveParentId(validatedProjectId, request.parentId());
    String normalizedName = requireNormalizedText(request.normalizedName(), "name", 255);
    FileType validatedType = requireType(request.type());

    ensureNameAvailable(validatedProjectId, validatedParentId, normalizedName, null);

    FlowFile flowFile =
        FlowFile.create(validatedProjectId, validatedParentId, normalizedName, validatedType);
    FlowFile savedFile = flowFileRepository.save(flowFile);
    return FileResponse.from(savedFile);
  }

  @Transactional(readOnly = true)
  public List<FileResponse> getFiles(Long projectId) {
    Long validatedProjectId = requirePositiveId(projectId, "projectId");
    List<FlowFile> files =
        flowFileRepository.findAllByProjectIdOrderByParentIdAscNameAsc(validatedProjectId);
    return files.stream().map(FileResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public FileResponse getFile(Long projectId, Long fileId) {
    return FileResponse.from(findFile(projectId, fileId));
  }

  @Transactional
  public FileResponse updateFile(Long projectId, Long fileId, UpdateFileRequest request) {
    if (!request.hasChanges()) {
      throw new IllegalArgumentException("at least one field must be provided");
    }

    FlowFile flowFile = findFile(projectId, fileId);
    String normalizedName = request.normalizedName();
    if (normalizedName != null) {
      String validatedName = requireNormalizedText(normalizedName, "name", 255);
      ensureNameAvailable(
          flowFile.getProjectId(), flowFile.getParentId(), validatedName, flowFile.getId());
      flowFile.rename(validatedName);
    }

    return FileResponse.from(flowFile);
  }

  private Long resolveParentId(Long projectId, Long parentId) {
    if (parentId == null) {
      return null;
    }

    Long validatedParentId = requirePositiveId(parentId, "parentId");
    FlowFile parent = findFile(projectId, validatedParentId);
    if (parent.getType() != FileType.FOLDER) {
      throw new IllegalArgumentException("parent must be a folder");
    }
    return validatedParentId;
  }

  private FlowFile findFile(Long projectId, Long fileId) {
    Long validatedProjectId = requirePositiveId(projectId, "projectId");
    Long validatedFileId = requirePositiveId(fileId, "fileId");
    return flowFileRepository
        .findByIdAndProjectId(validatedFileId, validatedProjectId)
        .orElseThrow(() -> new FileNotFoundException(validatedFileId));
  }

  private void ensureNameAvailable(
      Long projectId, Long parentId, String name, Long excludedFileId) {
    boolean exists;
    if (parentId == null) {
      exists =
          excludedFileId == null
              ? flowFileRepository.existsByProjectIdAndParentIdIsNullAndName(projectId, name)
              : flowFileRepository.existsByProjectIdAndParentIdIsNullAndNameAndIdNot(
                  projectId, name, excludedFileId);
    } else {
      exists =
          excludedFileId == null
              ? flowFileRepository.existsByProjectIdAndParentIdAndName(projectId, parentId, name)
              : flowFileRepository.existsByProjectIdAndParentIdAndNameAndIdNot(
                  projectId, parentId, name, excludedFileId);
    }

    if (exists) {
      throw new DuplicateFileNameException(name);
    }
  }

  private Long requirePositiveId(Long value, String fieldName) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
    return value;
  }

  private String requireNormalizedText(String value, String fieldName, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    if (value.length() > maxLength) {
      throw new IllegalArgumentException(
          fieldName + " must be at most " + maxLength + " characters");
    }
    return value;
  }

  private FileType requireType(FileType type) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    return type;
  }
}
