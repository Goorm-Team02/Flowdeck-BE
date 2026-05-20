package com.flowdeck.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** GitHub Pages 배포용 정적 OpenAPI 문서를 생성한다. */
@DatabaseIntegrationTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "OPENAPI_OUTPUT", matches = ".+")
class OpenApiDocsExportIntegrationTest {

  private final MockMvc mockMvc;

  @Autowired
  OpenApiDocsExportIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void exportsOpenApiDocumentToConfiguredPath() throws Exception {
    MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

    Path outputPath = Path.of(System.getenv("OPENAPI_OUTPUT"));
    Path parent = outputPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(
        outputPath, result.getResponse().getContentAsString(), StandardCharsets.UTF_8);
  }
}
