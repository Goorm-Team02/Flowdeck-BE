package com.flowdeck.backend.auth.controller;

import com.flowdeck.backend.auth.dto.LoginRequest;
import com.flowdeck.backend.auth.dto.LoginResponse;
import com.flowdeck.backend.auth.dto.RefreshTokenRequest;
import com.flowdeck.backend.auth.dto.SignupRequest;
import com.flowdeck.backend.auth.dto.SignupResponse;
import com.flowdeck.backend.auth.dto.TokenRefreshResponse;
import com.flowdeck.backend.auth.service.AuthService;
import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "회원가입, 로그인, 토큰 재발급, 로그아웃 API")
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ApiResponse.success("회원가입이 완료되었습니다.", authService.signup(request));
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success("로그인이 완료되었습니다.", authService.login(request));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenRefreshResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.success("Access Token이 재발급되었습니다.", authService.refresh(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @AuthenticationPrincipal JwtAuthentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
    authService.logout(authentication.getUserId(), authorizationHeader);
    return ApiResponse.success("로그아웃이 완료되었습니다.", null);
  }
}
