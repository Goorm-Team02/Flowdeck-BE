package com.flowdeck.backend.project.domain;

import com.flowdeck.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 36)
  private String publicId;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectVisibility visibility;

  public Project(String title, String description, ProjectVisibility visibility) {
    this.publicId = UUID.randomUUID().toString();
    this.title = title;
    this.description = description;
    this.visibility = visibility;
  }

  public void update(String title, String description, ProjectVisibility visibility) {
    this.title = title;
    this.description = description;
    this.visibility = visibility;
  }
}
