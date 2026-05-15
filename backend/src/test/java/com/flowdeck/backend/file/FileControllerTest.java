package com.flowdeck.backend.file;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flowdeck.backend.file.controller.FileController;
import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.dto.CreateFileRequest;
import com.flowdeck.backend.file.dto.FileResponse;
import com.flowdeck.backend.file.dto.UpdateFileRequest;
import com.flowdeck.backend.file.exception.FileNotFoundException;
import com.flowdeck.backend.file.service.FileService;
import com.flowdeck.backend.global.error.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Mock private FileService fileService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FileController(fileService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void createFileReturnsCreated() throws Exception {
    CreateFileRequest request = new CreateFileRequest(null, "Main.java", FileType.FILE);
    FileResponse response =
        new FileResponse(
            1L,
            10L,
            null,
            "Main.java",
            FileType.FILE,
            0,
            LocalDateTime.of(2026, 5, 15, 12, 0),
            LocalDateTime.of(2026, 5, 15, 12, 0));

    when(fileService.createFile(10L, request)).thenReturn(response);

    mockMvc
        .perform(
            post("/api/projects/10/files")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileId").value(1))
        .andExpect(jsonPath("$.projectId").value(10))
        .andExpect(jsonPath("$.type").value("FILE"))
        .andExpect(jsonPath("$.currentVersion").value(0));
  }

  @Test
  void getFilesReturnsList() throws Exception {
    FileResponse response =
        new FileResponse(
            1L,
            10L,
            null,
            "src",
            FileType.FOLDER,
            0,
            LocalDateTime.of(2026, 5, 15, 12, 0),
            LocalDateTime.of(2026, 5, 15, 12, 0));

    when(fileService.getFiles(10L)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/projects/10/files"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("src"))
        .andExpect(jsonPath("$[0].type").value("FOLDER"));
  }

  @Test
  void missingFileReturnsNotFound() throws Exception {
    when(fileService.getFile(10L, 99L)).thenThrow(new FileNotFoundException(99L));

    mockMvc
        .perform(get("/api/projects/10/files/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("file not found: 99"));
  }

  @Test
  void updateFileReturnsUpdatedFile() throws Exception {
    UpdateFileRequest request = new UpdateFileRequest("Flow-v2.java");
    FileResponse response =
        new FileResponse(
            1L,
            10L,
            null,
            "Flow-v2.java",
            FileType.FILE,
            0,
            LocalDateTime.of(2026, 5, 15, 12, 0),
            LocalDateTime.of(2026, 5, 15, 12, 30));

    when(fileService.updateFile(10L, 1L, request)).thenReturn(response);

    mockMvc
        .perform(
            patch("/api/projects/10/files/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Flow-v2.java"))
        .andExpect(jsonPath("$.type").value("FILE"));
  }
}
