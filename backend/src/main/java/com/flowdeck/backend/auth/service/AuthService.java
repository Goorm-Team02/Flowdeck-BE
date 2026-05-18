package com.flowdeck.backend.auth.service;

import com.flowdeck.backend.auth.dto.LoginRequest;
import com.flowdeck.backend.auth.dto.LoginResponse;
import com.flowdeck.backend.auth.dto.SignupRequest;
import com.flowdeck.backend.auth.dto.SignupResponse;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
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
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    String accessToken =
        jwtTokenProvider.createAccessToken(user.getId(), user.getPublicId(), List.of("ROLE_USER"));

    return LoginResponse.bearer(accessToken);
  }
}
