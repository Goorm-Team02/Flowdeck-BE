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
            "frontend project", "frontend-contract@test.com", "Main.java", "class Main {}", 1);

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
  void getFileTreeReturnsRevisionNeededForDelete() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "frontend tree project",
            "frontend-tree-contract@test.com",
            "Main.java",
            "class Main {}",
            1);

    mockMvc
        .perform(
            get("/api/projects/{projectId}/files", fixture.project().getPublicId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data[0].name").value("Main.java"))
        .andExpect(jsonPath("$.data[0].currentVersion").value(0))
        .andExpect(jsonPath("$.data[0].editRevision").value(1));
  }

  @Test
  void saveFileReturnsStateForFrontendRefresh() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "frontend project", "frontend-contract@test.com", "Main.java", "class Main {}", 1);

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
            "frontend project", "frontend-contract@test.com", "Main.java", "class Main {}", 1);

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
            "frontend project", "frontend-contract@test.com", "Main.java", "class Main {}", 1);
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

  @Test
  void getTimelineReturnsVersionMetadataForInitialTimelineView() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "timeline frontend project", "timeline-frontend-owner@test.com", "Editor.jsx", null, 0);
    fileVersionRepository.save(
        new FileVersion(
            fixture.file(),
            fixture.user().getId(),
            1,
            "import React from 'react'\n"
                + "\n"
                + "export default function Editor() {\n"
                + "  return <div>에디터</div>\n"
                + "}",
            "최초 생성"));
    fileVersionRepository.save(
        new FileVersion(
            fixture.file(),
            fixture.user().getId(),
            2,
            "import React from 'react'\n"
                + "import MonacoEditor from '@monaco-editor/react'\n"
                + "\n"
                + "export default function Editor() {\n"
                + "  return <MonacoEditor height=\"100%\" />\n"
                + "}",
            "Monaco 연결"));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}/versions/timeline",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.fileName").value("Editor.jsx"))
        .andExpect(jsonPath("$.data.totalVersions").value(2))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.versions[0].versionNumber").value(1))
        .andExpect(jsonPath("$.data.versions[0].changeMessage").value("최초 생성"))
        .andExpect(jsonPath("$.data.versions[0].createdByName").value("owner"))
        .andExpect(jsonPath("$.data.versions[1].versionNumber").value(2))
        .andExpect(jsonPath("$.data.versions[1].changeMessage").value("Monaco 연결"))
        .andExpect(jsonPath("$.data.versions[1].createdByName").value("owner"))
        .andExpect(jsonPath("$.data.selectedVersion").doesNotExist())
        .andExpect(jsonPath("$.data.diffFromPrevious").doesNotExist())
        .andExpect(jsonPath("$.data.versions[0].addedLinesFromPrevious").doesNotExist())
        .andExpect(jsonPath("$.data.versions[1].removedLinesFromPrevious").doesNotExist());
  }

  @Test
  void getTimelineReturnsEmptyStateWhenNoVersionsExist() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "empty timeline frontend project",
            "empty-timeline-owner@test.com",
            "Empty.java",
            null,
            0);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}/versions/timeline",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.totalVersions").value(0))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.versions.length()").value(0));
  }

  @Test
  void getDiffReturnsLimitResponseWhenVersionContentIsTooLarge() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "diff limit frontend project", "diff-limit-owner@test.com", "Editor.jsx", null, 0);
    fileVersionRepository.save(
        new FileVersion(fixture.file(), fixture.user().getId(), 1, "line 1", "v1"));
    fileVersionRepository.save(
        new FileVersion(
            fixture.file(), fixture.user().getId(), 2, "line\n".repeat(5_000) + "line", "v2"));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}/versions/diff?from=1&to=2",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("VERSION_413"))
        .andExpect(jsonPath("$.data.fromVersion").value(1))
        .andExpect(jsonPath("$.data.toVersion").value(2))
        .andExpect(jsonPath("$.data.maxLines").value(5000))
        .andExpect(jsonPath("$.data.toLineCount").value(5001));
  }

  @Test
  void getTimelineRejectsNonMember() throws Exception {
    FileFixture fixture =
        createOwnerFile(
            "forbidden timeline project",
            "timeline-owner@test.com",
            "Editor.jsx",
            "class Main {}",
            0);
    Long outsiderId =
        userRepository
            .save(
                new com.flowdeck.backend.user.domain.User(
                    "outsider@test.com", "password", "outsider"))
            .getId();

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}/versions/timeline",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(outsiderId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("AUTH_403"));
  }
}
