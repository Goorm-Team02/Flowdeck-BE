package com.flowdeck.backend.auth.service;

import com.flowdeck.backend.auth.dto.LoginRequest;
import com.flowdeck.backend.auth.dto.LoginResponse;
import com.flowdeck.backend.auth.dto.RefreshTokenRequest;
import com.flowdeck.backend.auth.dto.SignupRequest;
import com.flowdeck.backend.auth.dto.SignupResponse;
import com.flowdeck.backend.auth.dto.TokenRefreshResponse;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

  private static final String BEARER_PREFIX = "Bearer ";
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthTokenService authTokenService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      AuthTokenService authTokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.authTokenService = authTokenService;
  }

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    User user =
        new User(
            request.getEmail(), passwordEncoder.encode(request.getPassword()), request.getName());

    User savedUser = userRepository.save(user);
    return SignupResponse.from(savedUser);
  }

  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailAndDeletedAtIsNull(request.getEmail())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    String accessToken =
        jwtTokenProvider.createAccessToken(user.getId(), user.getPublicId(), List.of("ROLE_USER"));

    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getPublicId());

    authTokenService.saveRefreshToken(
        user.getId(), refreshToken, jwtTokenProvider.getRemainingDuration(refreshToken));

    return LoginResponse.bearer(accessToken, refreshToken);
  }

  @Transactional(readOnly = true)
  public TokenRefreshResponse refresh(RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();

    jwtTokenProvider.validateRefreshToken(refreshToken);
    Long userId = jwtTokenProvider.getUserId(refreshToken);

    if (!authTokenService.matchesRefreshToken(userId, refreshToken)) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    User user =
        userRepository
            .findById(userId)
            .filter(foundUser -> !foundUser.isDeleted())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

    String accessToken =
        jwtTokenProvider.createAccessToken(user.getId(), user.getPublicId(), List.of("ROLE_USER"));

    return TokenRefreshResponse.bearer(accessToken);
  }

  @Transactional(readOnly = true)
  public void logout(Long userId, String authorizationHeader) {
    String accessToken = resolveBearerToken(authorizationHeader);
    Duration remainingDuration = jwtTokenProvider.getRemainingDuration(accessToken);

    authTokenService.blacklistAccessToken(accessToken, remainingDuration);
    authTokenService.deleteRefreshToken(userId);
  }

  private String resolveBearerToken(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader)
        || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    return authorizationHeader.substring(BEARER_PREFIX.length());
  }
}
