package com.flowdeck.backend.projectmessage.domain;

import com.flowdeck.backend.global.entity.BaseTimeEntity;
import com.flowdeck.backend.project.domain.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMessage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 프로젝트 FK
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  // 작성자 FK, LOG 메시지는 null 가능
  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false, length = 10)
  private ProjectMessageType messageType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  private ProjectMessage(
      Project project, Long userId, ProjectMessageType messageType, String content) {
    this.project = project;
    this.userId = userId;
    this.messageType = messageType;
    this.content = content;
  }

  public static ProjectMessage chat(Project project, Long userId, String content) {
    return new ProjectMessage(project, userId, ProjectMessageType.CHAT, content);
  }

  public static ProjectMessage log(Project project, String content) {
    return new ProjectMessage(project, null, ProjectMessageType.LOG, content);
  }

  public boolean isWrittenBy(Long userId) {
    return this.userId != null && this.userId.equals(userId);
  }
}
