package com.flowdeck.backend.user.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.user.dto.UserResponse;
import com.flowdeck.backend.user.dto.UserUpdateRequest;
import com.flowdeck.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Users", description = "내 사용자 정보 조회, 수정, 회원 탈퇴 API")
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
  @Operation(
      summary = "회원 탈퇴",
      description =
          "현재 인증된 사용자를 탈퇴 처리합니다. 사용자 row는 삭제하지 않고 deletedAt을 설정하며, "
              + "email/name/passwordHash를 마스킹합니다. "
              + "탈퇴 성공 후 refresh token 삭제, 현재 access token blacklist 등록, force logout 설정을 수행합니다. "
              + "마지막 OWNER로 남아 있는 프로젝트가 있으면 USER_409 응답을 반환합니다. "
              + "WebSocket presence 즉시 제거는 2차 작업 범위이며, 1차에서는 TTL 기반 자연 만료를 사용합니다.")
  public ApiResponse<Void> withdrawMyAccount(
      @AuthenticationPrincipal JwtAuthentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
    userService.withdrawMyAccount(authentication.getUserId(), authorizationHeader);
    return ApiResponse.success("회원 탈퇴가 완료되었습니다.", null);
  }
}
