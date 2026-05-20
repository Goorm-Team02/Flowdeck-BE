package com.flowdeck.backend.isolated;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.testsupport.IsolatedTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles({"test", "prod"})
@SpringBootTest(
    classes = IsolatedTestApplication.class,
    properties = {"JWT_SECRET=flowdeck-prod-test-jwt-secret-key-change-me-now-123456"})
@AutoConfigureMockMvc
class ProdSwaggerIntegrationTest {

  private final MockMvc mockMvc;

  @Autowired
  ProdSwaggerIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void openApiDocsAreDisabledInProd() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
  }

  @Test
  void swaggerUiIsDisabledInProd() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
  }
}
