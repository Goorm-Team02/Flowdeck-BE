package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.DiffLineResponse;
import com.flowdeck.backend.version.dto.FileConflictResponse;
import com.flowdeck.backend.version.dto.FileTimelineResponse;
import com.flowdeck.backend.version.dto.FileTimelineVersionResponse;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreRequest;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.backend.version.service.FileVersionService;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class FileVersionServiceTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final FileVersionService fileVersionService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @Autowired
  FileVersionServiceTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      FileVersionService fileVersionService,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.fileVersionService = fileVersionService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  @Test
  void createVersionSnapshotsCurrentContentAndIncreasesCurrentVersion() {
    Project project =
        projectRepository.save(
            new Project("version create project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("version-create-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));
    file.updateContent("class Main { void run() {} }");

    FileVersionCreateResponse response =
        fileVersionService.createVersion(
            project.getPublicId(), owner.getId(), file.getId(), createVersionRequest("first save"));

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(response.currentVersion()).isEqualTo(1);
    assertThat(response.editRevision()).isZero();
    assertThat(file.getCurrentVersion()).isEqualTo(1);
    assertThat(versions).hasSize(1);
    assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
    assertThat(versions.get(0).getContent()).isEqualTo("class Main { void run() {} }");
    assertThat(versions.get(0).getChangeMessage()).isEqualTo("first save");
    assertThat(versions.get(0).getUserId()).isEqualTo(owner.getId());
  }

  @Test
  void restoreVersionCreatesNewVersionWithoutChangingPreviousVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("restore-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    file.increaseVersion();
    FileVersion firstVersion =
        fileVersionRepository.save(new FileVersion(file, null, 1, "v1 content", "first save"));

    file.increaseVersion();
    fileVersionRepository.save(new FileVersion(file, null, 2, "v2 content", "second save"));

    FileVersionRestoreResponse response =
        fileVersionService.restoreVersion(
            project.getPublicId(),
            owner.getId(),
            file.getId(),
            firstVersion.getId(),
            createRestoreRequest(0L));

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(response.currentVersion()).isEqualTo(3);
    assertThat(response.editRevision()).isEqualTo(1);
    assertThat(file.getCurrentVersion()).isEqualTo(3);
    assertThat(file.getEditRevision()).isEqualTo(1);
    assertThat(file.getCurrentContent()).isEqualTo("v1 content");
    assertThat(versions).hasSize(3);
    assertThat(versions).extracting(FileVersion::getVersionNumber).containsExactly(3, 2, 1);
    assertThat(versions.get(0).getUserId()).isEqualTo(owner.getId());
    assertThat(versions.get(0).getContent()).isEqualTo("v1 content");
    assertThat(versions.get(1).getContent()).isEqualTo("v2 content");
    assertThat(versions.get(2).getContent()).isEqualTo("v1 content");
  }

  @Test
  void restoreVersionFailsWhenBaseRevisionDoesNotMatchCurrentRevision() {
    Project project =
        projectRepository.save(
            new Project("restore conflict project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("restore-conflict-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    file.updateContent("current content");
    file.increaseEditRevision();
    file.increaseVersion();
    FileVersion version =
        fileVersionRepository.save(
            new FileVersion(file, owner.getId(), 1, "v1 content", "first save"));

    Throwable throwable =
        catchThrowable(
            () ->
                fileVersionService.restoreVersion(
                    project.getPublicId(),
                    owner.getId(),
                    file.getId(),
                    version.getId(),
                    createRestoreRequest(0L)));

    assertThat(throwable).isInstanceOf(BusinessException.class);

    BusinessException exception = (BusinessException) throwable;
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_EDIT_CONFLICT);
    assertThat(exception.getData()).isInstanceOf(FileConflictResponse.class);

    FileConflictResponse response = (FileConflictResponse) exception.getData();
    assertThat(response.fileId()).isEqualTo(file.getId());
    assertThat(response.baseRevision()).isZero();
    assertThat(response.currentRevision()).isEqualTo(1);
    assertThat(response.currentVersion()).isEqualTo(1);
    assertThat(response.latestContent()).isEqualTo("current content");
  }

  @Test
  void viewerCannotRestoreVersion() {
    Project project =
        projectRepository.save(
            new Project("viewer restore project", "description", ProjectVisibility.PRIVATE));
    User viewer = userRepository.save(new User("restore-viewer@test.com", "password", "viewer"));
    projectMemberRepository.save(new ProjectMember(project, viewer, ProjectRole.VIEWER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    file.increaseVersion();
    FileVersion version =
        fileVersionRepository.save(
            new FileVersion(file, viewer.getId(), 1, "v1 content", "first save"));

    assertThatThrownBy(
            () ->
                fileVersionService.restoreVersion(
                    project.getPublicId(),
                    viewer.getId(),
                    file.getId(),
                    version.getId(),
                    createRestoreRequest(0L)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void getVersionFailsWhenVersionDoesNotExist() {
    Project project =
        projectRepository.save(
            new Project("missing version project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("missing-version-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    assertThatThrownBy(
            () ->
                fileVersionService.getVersion(
                    project.getPublicId(), owner.getId(), file.getId(), 999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VERSION_NOT_FOUND);
  }

  @Test
  void getDiffAlignsInsertedLinesWithoutMarkingFollowingLinesChanged() {
    DiffFixture fixture =
        createDiffFixture(
            "insert-diff-owner@test.com",
            "import React from 'react'\n"
                + "\n"
                + "export default function Editor() {\n"
                + "  return <div>에디터</div>\n"
                + "}",
            "import React from 'react'\n"
                + "import MonacoEditor from '@monaco-editor/react'\n"
                + "\n"
                + "export default function Editor() {\n"
                + "  return <MonacoEditor height=\"100%\" />\n"
                + "}");

    FileVersionDiffResponse response =
        fileVersionService.getDiff(
            fixture.project().getPublicId(), fixture.owner().getId(), fixture.file().getId(), 1, 2);

    assertThat(response.addedLines()).isEqualTo(2);
    assertThat(response.removedLines()).isEqualTo(1);
    assertThat(response.changes())
        .extracting(DiffLineResponse::type, DiffLineResponse::content)
        .containsExactly(
            tuple("UNCHANGED", "import React from 'react'"),
            tuple("ADDED", "import MonacoEditor from '@monaco-editor/react'"),
            tuple("UNCHANGED", ""),
            tuple("UNCHANGED", "export default function Editor() {"),
            tuple("REMOVED", "  return <div>에디터</div>"),
            tuple("ADDED", "  return <MonacoEditor height=\"100%\" />"),
            tuple("UNCHANGED", "}"));
  }

  @Test
  void getDiffKeepsUnchangedTailAfterMiddleDeletion() {
    DiffFixture fixture =
        createDiffFixture(
            "delete-diff-owner@test.com",
            "line 1\n" + "remove me\n" + "line 2\n" + "line 3",
            "line 1\n" + "line 2\n" + "line 3");

    FileVersionDiffResponse response =
        fileVersionService.getDiff(
            fixture.project().getPublicId(), fixture.owner().getId(), fixture.file().getId(), 1, 2);

    assertThat(response.addedLines()).isZero();
    assertThat(response.removedLines()).isEqualTo(1);
    assertThat(response.changes())
        .extracting(DiffLineResponse::type, DiffLineResponse::content)
        .containsExactly(
            tuple("UNCHANGED", "line 1"),
            tuple("REMOVED", "remove me"),
            tuple("UNCHANGED", "line 2"),
            tuple("UNCHANGED", "line 3"));
  }

  @Test
  void getDiffShowsChangedLineAsRemovedAndAddedWithoutBreakingSurroundingLines() {
    DiffFixture fixture =
        createDiffFixture(
            "replace-diff-owner@test.com",
            "useEffect(() -> {\n"
                + "  const provider = createProvider()\n"
                + "  return () -> provider.disconnect()\n"
                + "}, [])",
            "useEffect(() -> {\n"
                + "  const provider = createProvider()\n"
                + "  return () -> provider.destroy()\n"
                + "}, [])");

    FileVersionDiffResponse response =
        fileVersionService.getDiff(
            fixture.project().getPublicId(), fixture.owner().getId(), fixture.file().getId(), 1, 2);

    assertThat(response.addedLines()).isEqualTo(1);
    assertThat(response.removedLines()).isEqualTo(1);
    assertThat(response.changes())
        .extracting(DiffLineResponse::type, DiffLineResponse::content)
        .containsExactly(
            tuple("UNCHANGED", "useEffect(() -> {"),
            tuple("UNCHANGED", "  const provider = createProvider()"),
            tuple("REMOVED", "  return () -> provider.disconnect()"),
            tuple("ADDED", "  return () -> provider.destroy()"),
            tuple("UNCHANGED", "}, [])"));
  }

  @Test
  void getDiffReturnsOnlyUnchangedLinesWhenContentsAreEqual() {
    DiffFixture fixture =
        createDiffFixture(
            "same-diff-owner@test.com",
            "const editor = createEditor()\neditor.focus()",
            "const editor = createEditor()\neditor.focus()");

    FileVersionDiffResponse response =
        fileVersionService.getDiff(
            fixture.project().getPublicId(), fixture.owner().getId(), fixture.file().getId(), 1, 2);

    assertThat(response.addedLines()).isZero();
    assertThat(response.removedLines()).isZero();
    assertThat(response.changes())
        .extracting(DiffLineResponse::type, DiffLineResponse::content)
        .containsExactly(
            tuple("UNCHANGED", "const editor = createEditor()"),
            tuple("UNCHANGED", "editor.focus()"));
  }

  @Test
  void getTimelineReturnsVersionMetadataWithoutLoadingContentAndDiff() {
    Project project =
        projectRepository.save(
            new Project("timeline project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("timeline-service-owner@test.com", "password", "홍길동"));
    User editor =
        userRepository.save(new User("timeline-service-editor@test.com", "password", "김철수"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(project, editor, ProjectRole.EDITOR));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Editor.jsx", FileType.FILE));

    fileVersionRepository.save(
        new FileVersion(
            file,
            owner.getId(),
            1,
            "import React from 'react'\n"
                + "\n"
                + "export default function Editor() {\n"
                + "  return <div>에디터</div>\n"
                + "}",
            "최초 생성"));
    fileVersionRepository.save(
        new FileVersion(
            file,
            editor.getId(),
            2,
            "import React from 'react'\n"
                + "import MonacoEditor from '@monaco-editor/react'\n"
                + "\n"
                + "export default function Editor() {\n"
                + "  return <MonacoEditor height=\"100%\" />\n"
                + "}",
            "Monaco 연결"));

    FileTimelineResponse response =
        fileVersionService.getTimeline(project.getPublicId(), owner.getId(), file.getId(), 0, 200);

    assertThat(response.fileId()).isEqualTo(file.getId());
    assertThat(response.fileName()).isEqualTo("Editor.jsx");
    assertThat(response.totalVersions()).isEqualTo(2);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(100);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.versions())
        .extracting(
            version -> version.versionNumber(),
            version -> version.changeMessage(),
            version -> version.createdBy(),
            version -> version.createdByName())
        .containsExactly(
            tuple(1, "최초 생성", owner.getId(), "홍길동"), tuple(2, "Monaco 연결", editor.getId(), "김철수"));
  }

  @Test
  void getTimelineReturnsRequestedPageAndHasNext() {
    Project project =
        projectRepository.save(
            new Project("timeline page project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("timeline-page-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Editor.jsx", FileType.FILE));
    for (int versionNumber = 1; versionNumber <= 5; versionNumber++) {
      fileVersionRepository.save(
          new FileVersion(
              file, owner.getId(), versionNumber, "line " + versionNumber, "v" + versionNumber));
    }

    FileTimelineResponse firstPage =
        fileVersionService.getTimeline(project.getPublicId(), owner.getId(), file.getId(), 0, 2);
    FileTimelineResponse lastPage =
        fileVersionService.getTimeline(project.getPublicId(), owner.getId(), file.getId(), 2, 2);
    FileTimelineResponse overPage =
        fileVersionService.getTimeline(project.getPublicId(), owner.getId(), file.getId(), 3, 2);

    assertThat(firstPage.totalVersions()).isEqualTo(5);
    assertThat(firstPage.page()).isZero();
    assertThat(firstPage.size()).isEqualTo(2);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.versions())
        .extracting(FileTimelineVersionResponse::versionNumber)
        .containsExactly(1, 2);

    assertThat(lastPage.totalVersions()).isEqualTo(5);
    assertThat(lastPage.page()).isEqualTo(2);
    assertThat(lastPage.size()).isEqualTo(2);
    assertThat(lastPage.hasNext()).isFalse();
    assertThat(lastPage.versions())
        .extracting(FileTimelineVersionResponse::versionNumber)
        .containsExactly(5);

    assertThat(overPage.totalVersions()).isEqualTo(5);
    assertThat(overPage.page()).isEqualTo(3);
    assertThat(overPage.size()).isEqualTo(2);
    assertThat(overPage.hasNext()).isFalse();
    assertThat(overPage.versions()).isEmpty();
  }

  @Test
  void getTimelineReturnsEmptyStateWhenFileHasNoSavedVersions() {
    Project project =
        projectRepository.save(
            new Project("empty timeline project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("empty-timeline-service-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Empty.java", FileType.FILE));

    FileTimelineResponse response =
        fileVersionService.getTimeline(project.getPublicId(), owner.getId(), file.getId(), -1, 0);

    assertThat(response.fileId()).isEqualTo(file.getId());
    assertThat(response.totalVersions()).isZero();
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.versions()).isEmpty();
  }

  @Test
  void getTimelineUsesFallbackNamesForSystemAndUnknownActors() {
    Project project =
        projectRepository.save(
            new Project("timeline actor project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("timeline-actor-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Editor.jsx", FileType.FILE));

    fileVersionRepository.save(new FileVersion(file, null, 1, "line 1", "system snapshot"));
    fileVersionRepository.save(
        new FileVersion(file, 999_999L, 2, "line 1\nline 2", "unknown actor"));

    FileTimelineResponse response =
        fileVersionService.getTimeline(project.getPublicId(), owner.getId(), file.getId(), 0, 20);

    assertThat(response.versions())
        .extracting(version -> version.versionNumber(), version -> version.createdByName())
        .containsExactly(tuple(1, "시스템"), tuple(2, "알 수 없음"));
  }

  private FileVersionCreateRequest createVersionRequest(String changeMessage) {
    FileVersionCreateRequest request = new FileVersionCreateRequest();
    ReflectionTestUtils.setField(request, "changeMessage", changeMessage);
    return request;
  }

  private FileVersionRestoreRequest createRestoreRequest(Long baseRevision) {
    FileVersionRestoreRequest request = new FileVersionRestoreRequest();
    ReflectionTestUtils.setField(request, "baseRevision", baseRevision);
    return request;
  }

  private DiffFixture createDiffFixture(
      String ownerEmail, String firstContent, String secondContent) {
    Project project =
        projectRepository.save(
            new Project("diff project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User(ownerEmail, "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Editor.jsx", FileType.FILE));

    fileVersionRepository.save(new FileVersion(file, owner.getId(), 1, firstContent, "v1"));
    fileVersionRepository.save(new FileVersion(file, owner.getId(), 2, secondContent, "v2"));

    return new DiffFixture(project, owner, file);
  }

  private record DiffFixture(Project project, User owner, ProjectFile file) {}
}
