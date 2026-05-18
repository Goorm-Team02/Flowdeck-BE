package com.flowdeck.backend.version.domain;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "file_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "version_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileVersion extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "file_id", nullable = false)
  private ProjectFile file;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "change_message", length = 255)
  private String changeMessage;

  public FileVersion(
      ProjectFile file, Long userId, int versionNumber, String content, String changeMessage) {
    this.file = file;
    this.userId = userId;
    this.versionNumber = versionNumber;
    this.content = content;
    this.changeMessage = changeMessage;
  }
}
