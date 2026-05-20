package com.flowdeck.backend.integration.file;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.testsupport.AuthenticatedWebIntegrationTest;
import com.flowdeck.testsupport.FileWebTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AuthenticatedWebIntegrationTest
class FileFrontendContractControllerTest extends FileWebTestSupport {

  private final MockMvc mockMvc;

  @Autowired
  FileFrontendContractControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void getFileReturnsEditorInitialStateFields() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "frontend project",
            "frontend-contract@test.com",
            "Main.java",
            "class Main {}",
            1);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.parentId").doesNotExist())
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.type").value("FILE"))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.editRevision").value(1))
        .andExpect(jsonPath("$.data.content").value("class Main {}"))
        .andExpect(jsonPath("$.data.createdAt").exists())
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  @Test
  void saveFileReturnsStateForFrontendRefresh() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "frontend project",
            "frontend-contract@test.com",
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
                      "content": "class Main { void run() {} }",
                      "baseRevision": 1,
                      "changeMessage": "save current content"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.editRevision").value(2))
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  @Test
  void createVersionReturnsStateForFrontendRefresh() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "frontend project",
            "frontend-contract@test.com",
            "Main.java",
            "class Main {}",
            1);

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectId}/files/{fileId}/versions",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"changeMessage\":\"first version\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.currentVersion").value(1))
        .andExpect(jsonPath("$.data.editRevision").value(1))
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  @Test
  void restoreVersionReturnsStateForFrontendRefresh() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "frontend project",
            "frontend-contract@test.com",
            "Main.java",
            "class Main {}",
            1);
    fixture.file().increaseVersion();
    projectFileRepository.save(fixture.file());

    FileVersion version =
        fileVersionRepository.save(
            new FileVersion(fixture.file(), fixture.user().getId(), 1, "class Restored {}", "v1"));

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectId}/files/{fileId}/versions/{versionId}/restore",
                    fixture.project().getPublicId(),
                    fixture.file().getId(),
                    version.getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseRevision\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.currentVersion").value(2))
        .andExpect(jsonPath("$.data.editRevision").value(2))
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }
}
