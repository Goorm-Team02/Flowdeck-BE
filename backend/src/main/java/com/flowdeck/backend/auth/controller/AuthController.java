package com.flowdeck.backend.auth.controller;

import com.flowdeck.backend.auth.dto.LoginRequest;
import com.flowdeck.backend.auth.dto.LoginResponse;
import com.flowdeck.backend.auth.dto.SignupRequest;
import com.flowdeck.backend.auth.dto.SignupResponse;
import com.flowdeck.backend.auth.service.AuthService;
import com.flowdeck.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}
