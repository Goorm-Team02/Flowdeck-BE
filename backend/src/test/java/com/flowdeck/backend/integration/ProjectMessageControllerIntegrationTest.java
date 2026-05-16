package com.flowdeck.backend.integration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.repository.ProjectMessageRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@DatabaseIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.jpa.auditing.enabled=true")
class ProjectMessageControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final ProjectRepository projectRepository;
  private final ProjectMessageRepository projectMessageRepository;

  private Project project;

  @Autowired
  ProjectMessageControllerIntegrationTest(
      MockMvc mockMvc,
      JwtTokenProvider jwtTokenProvider,
      ProjectRepository projectRepository,
      ProjectMessageRepository projectMessageRepository) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
    this.projectRepository = projectRepository;
    this.projectMessageRepository = projectMessageRepository;
  }

  @BeforeEach
  void setUp() {
    projectMessageRepository.deleteAll();
    projectRepository.deleteAll();

    project =
        projectRepository.save(new Project("Flowdeck", "프로젝트 메시지 테스트", ProjectVisibility.PUBLIC));
  }

  @Test
  void createMessageStoresAuthenticatedUserMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/{projectId}/messages", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"첫 메시지\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("메시지가 저장되었습니다."))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.messageType").value("CHAT"))
        .andExpect(jsonPath("$.data.content").value("첫 메시지"));
  }

  @Test
  void getMessagesReturnsProjectMessagesInCreatedOrder() throws Exception {
    projectMessageRepository.save(ProjectMessage.chat(project, 1L, "첫 번째"));
    projectMessageRepository.save(ProjectMessage.log(project, "시스템 로그"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/messages", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.messages[0].content").value("첫 번째"))
        .andExpect(jsonPath("$.data.messages[0].messageType").value("CHAT"))
        .andExpect(jsonPath("$.data.messages[1].content").value("시스템 로그"))
        .andExpect(jsonPath("$.data.messages[1].userId").doesNotExist())
        .andExpect(jsonPath("$.data.messages[1].messageType").value("LOG"));
  }

  @Test
  void searchMessagesFiltersByKeywordIgnoringCase() throws Exception {
    projectMessageRepository.save(ProjectMessage.chat(project, 1L, "Deploy completed"));
    projectMessageRepository.save(ProjectMessage.chat(project, 2L, "Design review"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/messages/search", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(1L))
                .queryParam("keyword", "deploy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.messages.length()").value(1))
        .andExpect(jsonPath("$.data.messages[0].content").value("Deploy completed"));
  }

  @Test
  void deleteMessageAllowsOnlyAuthor() throws Exception {
    ProjectMessage message =
        projectMessageRepository.save(ProjectMessage.chat(project, 1L, "삭제 대상"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/messages/{messageId}",
                    project.getPublicId(),
                    message.getId())
                .header(AUTHORIZATION, bearerToken(2L)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_403"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/messages/{messageId}",
                    project.getPublicId(),
                    message.getId())
                .header(AUTHORIZATION, bearerToken(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."));
  }

  private String bearerToken(Long userId) {
    return "Bearer "
        + jwtTokenProvider.createAccessToken(
            userId, "tester" + userId + "@flowdeck.com", List.of("ROLE_USER"));
  }
}
