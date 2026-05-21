package com.flowdeck.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageCreateRequest;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageEventResponse;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageEventType;
import com.flowdeck.backend.projectmessage.realtime.ProjectMessageBroadcaster;
import com.flowdeck.backend.projectmessage.repository.ProjectMessageRepository;
import com.flowdeck.backend.projectmessage.service.ProjectMessageService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@DatabaseIntegrationTest
@Import(ProjectMessageServiceTest.TestPublisherConfig.class)
class ProjectMessageServiceTest {

  private final ProjectMessageService projectMessageService;
  private final ProjectRepository projectRepository;
  private final ProjectMessageRepository projectMessageRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final TestProjectMessageBroadcaster projectMessageBroadcaster;

  private Project project;
  private User owner;
  private User viewer;

  @Autowired
  ProjectMessageServiceTest(
      ProjectMessageService projectMessageService,
      ProjectRepository projectRepository,
      ProjectMessageRepository projectMessageRepository,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository,
      TestProjectMessageBroadcaster projectMessageBroadcaster) {
    this.projectMessageService = projectMessageService;
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
    viewer = userRepository.save(new User("message-viewer@test.com", "password", "viewer"));

    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(project, viewer, ProjectRole.VIEWER));
  }

  @Test
  void createMessageStoresAuthenticatedUserMessageAndBroadcastsCreatedEvent() {
    var response =
        projectMessageService.createMessage(
            project.getPublicId(), owner.getId(), new ProjectMessageCreateRequest("첫 메시지"));

    assertThat(response.userId()).isEqualTo(owner.getId());
    assertThat(response.senderName()).isEqualTo(owner.getName());
    assertThat(response.messageType().name()).isEqualTo("CHAT");
    assertThat(response.content()).isEqualTo("첫 메시지");
    assertThat(projectMessageRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()))
        .hasSize(1);

    assertThat(projectMessageBroadcaster.events()).hasSize(1);
    assertThat(projectMessageBroadcaster.events().getFirst().projectId())
        .isEqualTo(project.getPublicId());
    assertThat(projectMessageBroadcaster.events().getFirst().event().eventType())
        .isEqualTo(ProjectMessageEventType.CREATED);
    assertThat(projectMessageBroadcaster.events().getFirst().event().message().senderName())
        .isEqualTo(owner.getName());
    assertThat(projectMessageBroadcaster.events().getFirst().event().message().content())
        .isEqualTo("첫 메시지");
  }

  @Test
  void viewerCannotCreateMessage() {
    assertThatThrownBy(
            () ->
                projectMessageService.createMessage(
                    project.getPublicId(),
                    viewer.getId(),
                    new ProjectMessageCreateRequest("뷰어 메시지")))
        .isInstanceOf(BusinessException.class);
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
