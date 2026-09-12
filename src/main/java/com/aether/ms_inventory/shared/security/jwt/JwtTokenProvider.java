package com.aether.ms_inventory.shared.security.jwt;

import com.aether.ms_inventory.shared.security.dto.output.LoginOutputDTO;
import com.aether.ms_inventory.shared.exceptions.UnauthorizedException;
import com.aether.ms_inventory.shared.security.UserService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider {
  @Value("${spring.security.jwt.token.secret-key}")
  private String secretKey;

  @Value("${spring.security.jwt.token.expire-length}")
  private Integer validityInMilliseconds;

  private final UserService userService;

  Algorithm algorithm = null;

  @PostConstruct
  protected void init(){
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    algorithm = Algorithm.HMAC256(secretKey.getBytes());
  }

  public LoginOutputDTO createAccessToken(String email, List<String> permissions){
    Date now = new Date();
    Date validity = new Date(now.getTime() + validityInMilliseconds);

    String accessToken = getAccessToken(email, permissions, now, validity);
    String refreshToken = getRefreshToken(email, permissions, now);

    return new LoginOutputDTO(
        email,
        Boolean.TRUE,
        now,
        validity,
        accessToken,
        refreshToken
    );
  }

  public LoginOutputDTO refreshToken(String refreshToken){
    if (!StringUtils.isEmpty(refreshToken) && refreshToken.startsWith("Bearer ")) refreshToken = refreshToken.substring("Bearer ".length());

    JWTVerifier verifier = JWT.require(algorithm).build();

    DecodedJWT decodedJWT = verifier.verify(refreshToken);

    String email = decodedJWT.getSubject();

    List<String> permissions = decodedJWT.getClaim("roles").asList(String.class);

    return createAccessToken(email, permissions);
  }

  private String getRefreshToken(String email, List<String> permissions, Date now) {
    Date refreshTokenValidity = new Date(now.getTime() + validityInMilliseconds * 3);

    return JWT.create()
        .withClaim("roles", permissions)
        .withIssuedAt(now)
        .withExpiresAt(refreshTokenValidity)
        .withSubject(email)
        .sign(algorithm);
  }

  private String getAccessToken(String email, List<String> permissions, Date now, Date validity) {
    String issuerUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();

    return JWT.create()
        .withClaim("roles", permissions)
        .withIssuedAt(now)
        .withExpiresAt(validity)
        .withSubject(email)
        .withIssuer(issuerUrl)
        .sign(algorithm);
  }

  public Authentication getAuthentication(String token){
    DecodedJWT decodedJWT = decodedToken(token);

    UserDetails userDetails = this.userService
        .loadUserByUsername(decodedJWT.getSubject());

    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  private DecodedJWT decodedToken(String token) {
    Algorithm alg = Algorithm.HMAC256(secretKey.getBytes());

    JWTVerifier verifier = JWT.require(alg).build();

    try {
      return verifier.verify(token);
    } catch (JWTDecodeException | TokenExpiredException e) {
      throw new UnauthorizedException("exception.jwt.invalid-or-expired");
    }
  }

  public String resolveToken(HttpServletRequest request){
    String bearerToken = request.getHeader("Authorization");

    if (!StringUtils.isEmpty(bearerToken) && bearerToken.startsWith("Bearer ")) return bearerToken.substring("Bearer ".length());

    return null;
  }

  public boolean validateToken(String token){
    DecodedJWT decodedJWT = decodedToken(token);

    try {
      return decodedJWT.getExpiresAt().after(new Date());
    } catch (Exception e){
      throw new UnauthorizedException("exception.jwt.invalid-or-expired");
    }
  }
}
