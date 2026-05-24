package com.flowdeck.backend.user.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.user.dto.UserResponse;
import com.flowdeck.backend.user.dto.UserUpdateRequest;
import com.flowdeck.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Users", description = "내 사용자 정보 조회 및 수정 API")
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ApiResponse<UserResponse> getMyInfo(
      @AuthenticationPrincipal JwtAuthentication authentication) {
    return ApiResponse.success(userService.getMyInfo(authentication.getUserId()));
  }

  @PatchMapping("/me")
  public ApiResponse<UserResponse> updateMyInfo(
      @AuthenticationPrincipal JwtAuthentication authentication,
      @Valid @RequestBody UserUpdateRequest request) {
    return ApiResponse.success(
        "사용자 정보가 수정되었습니다.", userService.updateMyInfo(authentication.getUserId(), request));
  }

  @DeleteMapping("/me")
  public ApiResponse<Void> withdrawMyAccount(
      @AuthenticationPrincipal JwtAuthentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
    userService.withdrawMyAccount(authentication.getUserId(), authorizationHeader);
    return ApiResponse.success("회원 탈퇴가 완료되었습니다.", null);
  }
}
