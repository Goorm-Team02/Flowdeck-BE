package com.flowdeck.backend.integration.file;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.testsupport.AuthenticatedWebIntegrationTest;
import com.flowdeck.testsupport.FileWebTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AuthenticatedWebIntegrationTest
class FileConflictResponseControllerTest extends FileWebTestSupport {

  private final MockMvc mockMvc;

  @Autowired
  FileConflictResponseControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void saveFileConflictReturnsFileConflictResponseData() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "conflict project",
            "conflict-api@test.com",
            "Main.java",
            "class Main {}",
            1);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "stale content",
                      "baseRevision": 0,
                      "changeMessage": "stale save"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_409"))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.baseRevision").value(0))
        .andExpect(jsonPath("$.data.currentRevision").value(1))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.latestContent").value("class Main {}"));
  }

  @Test
  void deleteFileWithoutExpectedRevisionReturnsBadRequest() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "conflict project",
            "conflict-api@test.com",
            "Main.java",
            "class Main {}",
            1);

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COMMON_400"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}
