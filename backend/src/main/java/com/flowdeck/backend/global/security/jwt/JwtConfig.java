package com.flowdeck.backend.global.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.Assert;

@Configuration
public class JwtConfig {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  @Bean
  public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(jwtProperties)));
  }

  @Bean
  public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
    return NimbusJwtDecoder.withSecretKey(secretKey(jwtProperties))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  private SecretKey secretKey(JwtProperties jwtProperties) {
    byte[] secretKeyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    Assert.isTrue(secretKeyBytes.length >= 32, "JWT secret must be at least 32 bytes.");
    return new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
  }
}
