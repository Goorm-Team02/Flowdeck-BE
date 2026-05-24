package com.flowdeck.backend.user.domain;

import com.flowdeck.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 36)
  private String publicId;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public User(String email, String passwordHash, String name) {
    this.publicId = UUID.randomUUID().toString();
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
  }

  public void updateName(String name) {
    this.name = name;
  }

  public void updatePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void withdraw(String maskedEmail, String maskedName, String maskedPasswordHash) {
    this.email = maskedEmail;
    this.name = maskedName;
    this.passwordHash = maskedPasswordHash;
    this.deletedAt = LocalDateTime.now();
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
