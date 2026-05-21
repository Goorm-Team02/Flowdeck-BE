package com.flowdeck.testsupport;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class FileWebTestSupport extends JwtBearerTokenTestSupport {

  @Autowired protected ProjectRepository projectRepository;
  @Autowired protected ProjectFileRepository projectFileRepository;
  @Autowired protected ProjectMemberRepository projectMemberRepository;
  @Autowired protected UserRepository userRepository;
  @Autowired protected FileVersionRepository fileVersionRepository;

  @BeforeEach
  void clearFileWebTestData() {
    fileVersionRepository.deleteAll();
    projectFileRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected OwnerProjectFixture createOwnerProject(String projectTitle, String userEmail) {
    Project project =
        projectRepository.save(new Project(projectTitle, "description", ProjectVisibility.PRIVATE));
    User user = userRepository.save(new User(userEmail, "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, user, ProjectRole.OWNER));
    return new OwnerProjectFixture(project, user);
  }

  protected FileFixture createOwnerFile(
      String projectTitle, String userEmail, String fileName, String content, long editRevision) {
    OwnerProjectFixture fixture = createOwnerProject(projectTitle, userEmail);

    ProjectFile file = new ProjectFile(fixture.project(), null, fileName, FileType.FILE);
    if (content != null) {
      file.updateContent(content);
    }
    for (long revision = 0; revision < editRevision; revision += 1) {
      file.increaseEditRevision();
    }

    return new FileFixture(fixture.project(), fixture.user(), projectFileRepository.save(file));
  }

  protected record OwnerProjectFixture(Project project, User user) {}

  protected record FileFixture(Project project, User user, ProjectFile file) {}
}
