package com.flowdeck.backend.global.security.jwt;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtProperties jwtProperties;

  public JwtTokenProvider(
      JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.jwtProperties = jwtProperties;
  }

  public String createAccessToken(Long userId, String subject, Collection<String> roles) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("flowdeck")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds()))
            .subject(subject)
            .claim("userId", userId)
            .claim("roles", roles)
            .build();

    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public Authentication getAuthentication(String accessToken) {
    Jwt jwt = parse(accessToken);
    Number userIdClaim = jwt.getClaim("userId");
    JwtAuthentication principal =
        new JwtAuthentication(
            userIdClaim == null ? null : userIdClaim.longValue(), jwt.getSubject());

    return UsernamePasswordAuthenticationToken.authenticated(
        principal, null, extractAuthorities(jwt));
  }

  private Jwt parse(String accessToken) {
    try {
      return jwtDecoder.decode(accessToken);
    } catch (JwtException exception) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
  }

  private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    if (roles == null) {
      return List.of();
    }

    return roles.stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList();
  }
}
