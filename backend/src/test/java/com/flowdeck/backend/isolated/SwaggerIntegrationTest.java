package com.flowdeck.backend.isolated;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.testsupport.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** 격리된 웹 테스트 컨텍스트에서 Swagger UI와 OpenAPI 문서 엔드포인트가 정상 노출되는지 검증한다. */
@WebIntegrationTest
class SwaggerIntegrationTest {

  private final MockMvc mockMvc;

  @Autowired
  SwaggerIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void openApiDocsAreExposedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.info.title").value("FlowDeck API"))
        .andExpect(jsonPath("$.info.description").value("협업 중심 Web IDE 플랫폼 API 문서"))
        .andExpect(jsonPath("$.info.version").value("0.0.1-SNAPSHOT"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
  }

  @Test
  void swaggerUiIsExposedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"));
  }
}
