package com.flowdeck.backend.isolated;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.testsupport.WebIntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 공통 응답 포맷, 예외 처리, CORS, JWT 보안 등 전역 API 동작을 격리된 웹 테스트 컨텍스트에서 검증한다. */
@WebIntegrationTest
@Import(GlobalApiFoundationIntegrationTest.TestSupportConfig.class)
class GlobalApiFoundationIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;

  @Autowired
  GlobalApiFoundationIntegrationTest(MockMvc mockMvc, JwtTokenProvider jwtTokenProvider) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Test
  void publicEndpointReturnsCommonSuccessResponse() throws Exception {
    mockMvc
        .perform(get("/api/auth/login"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
        .andExpect(jsonPath("$.data").value("pong"));
  }

  @Test
  void businessExceptionIsHandledByRestControllerAdvice() throws Exception {
    mockMvc
        .perform(get("/api/auth/signup"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COMMON_404"))
        .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다."));
  }

  @Test
  void missingEndpointReturnsCommonNotFoundResponse() throws Exception {
    mockMvc
        .perform(get("/swagger-ui/does-not-exist.html"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COMMON_404"))
        .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다."));
  }

  @Test
  void validationExceptionIsHandledWithCommonErrorResponse() throws Exception {
    mockMvc
        .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COMMON_400"))
        .andExpect(jsonPath("$.data[0].field").value("name"));
  }

  @Test
  void corsConfigurationAllowsConfiguredOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/auth/ping")
                .header(ORIGIN, "http://localhost:3000")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
  }

  @Test
  void protectedEndpointRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/secure/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("AUTH_401"));
  }

  @Test
  void protectedEndpointAcceptsValidJwt() throws Exception {
    mockMvc
        .perform(
            get("/api/secure/profile")
                .header(AUTHORIZATION, "Bearer " + createAccessToken("ROLE_USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.name").value("tester@flowdeck.com"));
  }

  @Test
  void invalidJwtReturnsUnauthorizedResponse() throws Exception {
    mockMvc
        .perform(get("/api/secure/profile").header(AUTHORIZATION, "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("AUTH_401_1"));
  }

  @Test
  void insufficientRoleReturnsForbiddenResponse() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/ping")
                .header(AUTHORIZATION, "Bearer " + createAccessToken("ROLE_USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("AUTH_403"));
  }

  private String createAccessToken(String role) {
    return jwtTokenProvider.createAccessToken(1L, "tester@flowdeck.com", List.of(role));
  }

  @TestConfiguration
  @Import(TestSupportController.class)
  static class TestSupportConfig {

    protected TestSupportConfig() {
      super();
    }
  }

  @RestController
  static class TestSupportController {

    @GetMapping("/api/auth/login")
    ApiResponse<String> publicPing() {
      return ApiResponse.success("pong");
    }

    @GetMapping("/api/auth/signup")
    ApiResponse<Void> businessError() {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @PostMapping("/api/auth/refresh")
    ApiResponse<Map<String, String>> validationError(
        @Valid @RequestBody ValidationRequest request) {
      return ApiResponse.success(Map.of("name", request.getName()));
    }

    @GetMapping("/api/secure/profile")
    ApiResponse<Map<String, Object>> secureProfile(Authentication authentication) {
      JwtAuthentication principal = (JwtAuthentication) authentication.getPrincipal();
      return ApiResponse.success(
          Map.of("userId", principal.getUserId(), "name", principal.getName()));
    }

    @GetMapping("/api/admin/ping")
    ApiResponse<String> adminPing() {
      return ApiResponse.success("admin");
    }
  }

  static class ValidationRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
