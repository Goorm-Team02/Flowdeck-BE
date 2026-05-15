package com.flowdeck.backend.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.FlowFile;
import com.flowdeck.backend.file.dto.CreateFileRequest;
import com.flowdeck.backend.file.dto.FileResponse;
import com.flowdeck.backend.file.dto.UpdateFileRequest;
import com.flowdeck.backend.file.exception.DuplicateFileNameException;
import com.flowdeck.backend.file.repository.FlowFileRepository;
import com.flowdeck.backend.file.service.FileService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  @Mock private FlowFileRepository flowFileRepository;

  @InjectMocks private FileService fileService;

  @Test
  void createFileRejectsDuplicateRootName() {
    CreateFileRequest request = new CreateFileRequest(null, "Flow.java", FileType.FILE);
    when(flowFileRepository.existsByProjectIdAndParentIdIsNullAndName(1L, "Flow.java"))
        .thenReturn(true);

    assertThrows(DuplicateFileNameException.class, () -> fileService.createFile(1L, request));
  }

  @Test
  void createFilePersistsEntityAndReturnsResponse() {
    CreateFileRequest request = new CreateFileRequest(null, "  Flow.java  ", FileType.FILE);
    FlowFile savedFile = FlowFile.create(1L, null, "Flow.java", FileType.FILE);

    when(flowFileRepository.existsByProjectIdAndParentIdIsNullAndName(1L, "Flow.java"))
        .thenReturn(false);
    when(flowFileRepository.save(any(FlowFile.class))).thenReturn(savedFile);

    FileResponse response = fileService.createFile(1L, request);

    assertEquals(1L, response.projectId());
    assertEquals("Flow.java", response.name());
    verify(flowFileRepository)
        .save(
            argThat(
                flowFile ->
                    flowFile.getProjectId().equals(1L)
                        && flowFile.getParentId() == null
                        && flowFile.getName().equals("Flow.java")
                        && flowFile.getType() == FileType.FILE));
  }

  @Test
  void createChildFileRequiresFolderParent() {
    FlowFile parentFile = FlowFile.create(1L, null, "Main.java", FileType.FILE);
    CreateFileRequest request = new CreateFileRequest(10L, "Utils.java", FileType.FILE);

    ReflectionTestUtils.setField(parentFile, "id", 10L);
    when(flowFileRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(parentFile));

    assertThrows(IllegalArgumentException.class, () -> fileService.createFile(1L, request));
  }

  @Test
  void updateFileRenamesNodeWithinProject() {
    FlowFile flowFile = FlowFile.create(1L, null, "Flow.java", FileType.FILE);
    UpdateFileRequest request = new UpdateFileRequest("Flow-v2.java");

    ReflectionTestUtils.setField(flowFile, "id", 10L);
    when(flowFileRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(flowFile));
    when(flowFileRepository.existsByProjectIdAndParentIdIsNullAndNameAndIdNot(
            1L, "Flow-v2.java", 10L))
        .thenReturn(false);

    FileResponse response = fileService.updateFile(1L, 10L, request);

    assertEquals("Flow-v2.java", response.name());
    assertEquals(FileType.FILE, response.type());
  }

  @Test
  void updateFileRejectsBlankName() {
    FlowFile flowFile = FlowFile.create(1L, null, "Flow.java", FileType.FILE);
    UpdateFileRequest request = new UpdateFileRequest("   ");

    ReflectionTestUtils.setField(flowFile, "id", 10L);
    when(flowFileRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(flowFile));

    assertThrows(IllegalArgumentException.class, () -> fileService.updateFile(1L, 10L, request));
  }
}
