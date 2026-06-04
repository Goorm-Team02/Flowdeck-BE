package com.flowdeck.backend.user.service;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.security.jwt.JwtProperties;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.global.transaction.AfterCommitExecutor;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.presence.store.ProjectPresenceStore;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.dto.UserResponse;
import com.flowdeck.backend.user.dto.UserUpdateRequest;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String WITHDRAWN_USER_NAME = "탈퇴한 사용자";
  private static final DateTimeFormatter MASKED_EMAIL_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  private final AuthTokenService authTokenService;
  private final AfterCommitExecutor afterCommitExecutor;
  private final ProjectPresenceStore projectPresenceStore;

  public UserService(
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties,
      AuthTokenService authTokenService,
      AfterCommitExecutor afterCommitExecutor,
      ProjectPresenceStore projectPresenceStore) {
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
    this.authTokenService = authTokenService;
    this.afterCommitExecutor = afterCommitExecutor;
    this.projectPresenceStore = projectPresenceStore;
  }

  @Transactional(readOnly = true)
  public UserResponse getMyInfo(Long userId) {
    User user = getUser(userId);
    return UserResponse.from(user);
  }

  @Transactional
  public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
    User user = getUser(userId);
    user.updateName(request.getName());

    return UserResponse.from(user);
  }

  @Transactional
  public void withdrawMyAccount(Long userId, String authorizationHeader) {
    User user = getUser(userId);
    validateCanWithdraw(user);

    String accessToken = resolveBearerToken(authorizationHeader);
    Duration remainingDuration = jwtTokenProvider.getRemainingDuration(accessToken);

    projectMemberRepository.deleteAll(projectMemberRepository.findAllByUser(user));
    user.withdraw(
        createMaskedEmail(user.getId()),
        WITHDRAWN_USER_NAME,
        passwordEncoder.encode(UUID.randomUUID().toString()));

    afterCommitExecutor.run(
        () -> {
          authTokenService.deleteRefreshToken(userId);
          authTokenService.blacklistAccessToken(accessToken, remainingDuration);
          authTokenService.forceLogout(
              userId, Duration.ofSeconds(jwtProperties.getAccessTokenExpirationSeconds()));
          projectPresenceStore.removeSessionsByUserId(userId);
        });
  }

  private void validateCanWithdraw(User user) {
    boolean hasLastOwnerProject =
        projectMemberRepository.findAllByUserAndRole(user, ProjectRole.OWNER).stream()
            .map(ProjectMember::getProject)
            .anyMatch(
                project ->
                    projectMemberRepository.countByProjectAndRole(project, ProjectRole.OWNER) <= 1);

    if (hasLastOwnerProject) {
      throw new BusinessException(ErrorCode.USER_WITHDRAWAL_BLOCKED);
    }
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .filter(user -> !user.isDeleted())
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private String createMaskedEmail(Long userId) {
    return "deleted+"
        + userId
        + "+"
        + LocalDateTime.now().format(MASKED_EMAIL_TIMESTAMP_FORMATTER)
        + "@flowdeck.local";
  }

  private String resolveBearerToken(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader)
        || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    return authorizationHeader.substring(BEARER_PREFIX.length());
  }
}
