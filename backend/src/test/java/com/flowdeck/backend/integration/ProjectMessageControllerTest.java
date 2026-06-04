package com.flowdeck.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageEventResponse;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageEventType;
import com.flowdeck.backend.projectmessage.realtime.ProjectMessageBroadcaster;
import com.flowdeck.backend.projectmessage.repository.ProjectMessageRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.AuthenticatedWebIntegrationTest;
import com.flowdeck.testsupport.JwtBearerTokenTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AuthenticatedWebIntegrationTest
@TestPropertySource(properties = "app.jpa.auditing.enabled=true")
@Import(ProjectMessageControllerTest.TestPublisherConfig.class)
class ProjectMessageControllerTest extends JwtBearerTokenTestSupport {

  private final MockMvc mockMvc;
  private final ProjectRepository projectRepository;
  private final ProjectMessageRepository projectMessageRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final TestProjectMessageBroadcaster projectMessageBroadcaster;

  private Project project;
  private User owner;
  private User editor;
  private User viewer;
  private User outsider;

  @Autowired
  ProjectMessageControllerTest(
      MockMvc mockMvc,
      ProjectRepository projectRepository,
      ProjectMessageRepository projectMessageRepository,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository,
      TestProjectMessageBroadcaster projectMessageBroadcaster) {
    this.mockMvc = mockMvc;
    this.projectRepository = projectRepository;
    this.projectMessageRepository = projectMessageRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
    this.projectMessageBroadcaster = projectMessageBroadcaster;
  }

  @BeforeEach
  void setUp() {
    projectMessageRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
    projectMessageBroadcaster.reset();

    project =
        projectRepository.save(new Project("Flowdeck", "프로젝트 메시지 테스트", ProjectVisibility.PUBLIC));
    owner = userRepository.save(new User("message-owner@test.com", "password", "owner"));
    editor = userRepository.save(new User("message-editor@test.com", "password", "editor"));
    viewer = userRepository.save(new User("message-viewer@test.com", "password", "viewer"));
    outsider = userRepository.save(new User("message-outsider@test.com", "password", "outsider"));

    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(project, editor, ProjectRole.EDITOR));
    projectMemberRepository.save(new ProjectMember(project, viewer, ProjectRole.VIEWER));
  }

  @Test
  void getMessagesReturnsProjectMessagesInCreatedOrder() throws Exception {
    projectMessageRepository.save(ProjectMessage.chat(project, owner.getId(), "첫 번째"));
    projectMessageRepository.save(ProjectMessage.log(project, "시스템 로그"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/messages", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.messages[0].content").value("첫 번째"))
        .andExpect(jsonPath("$.data.messages[0].senderName").value(owner.getName()))
        .andExpect(jsonPath("$.data.messages[0].messageType").value("CHAT"))
        .andExpect(jsonPath("$.data.messages[1].content").value("시스템 로그"))
        .andExpect(jsonPath("$.data.messages[1].senderName").doesNotExist())
        .andExpect(jsonPath("$.data.messages[1].userId").doesNotExist())
        .andExpect(jsonPath("$.data.messages[1].messageType").value("LOG"));
  }

  @Test
  void searchMessagesFiltersByKeywordIgnoringCase() throws Exception {
    projectMessageRepository.save(ProjectMessage.chat(project, owner.getId(), "Deploy completed"));
    projectMessageRepository.save(ProjectMessage.chat(project, editor.getId(), "Design review"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/messages/search", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(viewer.getId()))
                .queryParam("keyword", "deploy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.messages.length()").value(1))
        .andExpect(jsonPath("$.data.messages[0].senderName").value(owner.getName()))
        .andExpect(jsonPath("$.data.messages[0].content").value("Deploy completed"));
  }

  @Test
  void deleteMessageAllowsOnlyAuthor() throws Exception {
    ProjectMessage message =
        projectMessageRepository.save(ProjectMessage.chat(project, owner.getId(), "삭제 대상"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/messages/{messageId}",
                    project.getPublicId(),
                    message.getId())
                .header(AUTHORIZATION, bearerToken(editor.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_403"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/messages/{messageId}",
                    project.getPublicId(),
                    message.getId())
                .header(AUTHORIZATION, bearerToken(owner.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."));

    assertThat(projectMessageBroadcaster.events()).hasSize(1);
    assertThat(projectMessageBroadcaster.events().getFirst().event().eventType())
        .isEqualTo(ProjectMessageEventType.DELETED);
    assertThat(projectMessageBroadcaster.events().getFirst().event().messageId())
        .isEqualTo(message.getId());
  }

  @Test
  void nonMemberCannotReadMessages() throws Exception {
    projectMessageRepository.save(ProjectMessage.chat(project, owner.getId(), "첫 번째"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/messages", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(outsider.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_403"));
  }

  @Test
  void deleteMessageRequiresProjectMembershipEvenForAuthor() throws Exception {
    ProjectMessage message =
        projectMessageRepository.save(ProjectMessage.chat(project, outsider.getId(), "삭제 대상"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/messages/{messageId}",
                    project.getPublicId(),
                    message.getId())
                .header(AUTHORIZATION, bearerToken(outsider.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_403"));

    assertThat(projectMessageRepository.existsById(message.getId())).isTrue();
  }

  @TestConfiguration
  static class TestPublisherConfig {

    @Bean
    @Primary
    TestProjectMessageBroadcaster projectMessageBroadcaster() {
      return new TestProjectMessageBroadcaster();
    }
  }

  static class TestProjectMessageBroadcaster implements ProjectMessageBroadcaster {

    private final List<PublishedEvent> events = new ArrayList<>();

    @Override
    public void broadcastCreated(
        String projectId, com.flowdeck.backend.projectmessage.dto.ProjectMessageResponse message) {
      events.add(new PublishedEvent(projectId, createdEvent(message)));
    }

    @Override
    public void broadcastDeleted(String projectId, Long messageId) {
      events.add(new PublishedEvent(projectId, deletedEvent(messageId)));
    }

    List<PublishedEvent> events() {
      return events;
    }

    void reset() {
      events.clear();
    }
  }

  record PublishedEvent(String projectId, ProjectMessageEventResponse event) {}
}
