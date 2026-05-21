package com.flowdeck.backend.integration.file;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.testsupport.AuthenticatedWebIntegrationTest;
import com.flowdeck.testsupport.FileWebTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AuthenticatedWebIntegrationTest
class FileErrorResponseControllerTest extends FileWebTestSupport {

  private final MockMvc mockMvc;

  @Autowired
  FileErrorResponseControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void createFileWithDuplicateNameReturnsFileNameDuplicated() throws Exception {
    OwnerProjectFixture fixture =
        createOwnerProject("error contract project", "file-error@test.com");
    projectFileRepository.save(
        new ProjectFile(fixture.project(), null, "Main.java", FileType.FILE));

    mockMvc
        .perform(
            post("/api/projects/{projectId}/files", fixture.project().getPublicId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Main.java",
                      "type": "FILE"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_409_1"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void saveOversizedFileReturnsFileSizeExceeded() throws Exception {
    OwnerProjectFixture fixture =
        createOwnerProject("error contract project", "file-error@test.com");
    ProjectFile file =
        projectFileRepository.save(
            new ProjectFile(fixture.project(), null, "Large.java", FileType.FILE));
    String oversizedContent = "a".repeat(1024 * 1024 + 1);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    file.getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "%s",
                      "baseRevision": 0
                    }
                    """
                        .formatted(oversizedContent)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_400_3"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}
