/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.authservice.web;

import com.rackspace.salus.authservice.services.TokenService;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Processes a bearer token provided in the Authorization header by validating it is
 * an allocated token and then sets the authenticated principal to the associated tenant ID.
 */
@Slf4j
public class EnvoyTokenAuthFilter extends OncePerRequestFilter {

  public static final String ROLE_CERT_REQUESTOR = "CERT_REQUESTOR";

  public static final GrantedAuthority CERT_REQUESTOR =
      new SimpleGrantedAuthority(ROLE_CERT_REQUESTOR);

  static final Collection<? extends GrantedAuthority> AUTHORITIES = List.of(CERT_REQUESTOR);

  // use a similar strategy as OAuth2's DefaultBearerTokenResolver
  private static final Pattern authorizationPattern = Pattern.compile(
      "^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$",
      Pattern.CASE_INSENSITIVE);

  private final TokenService tokenService;

  private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();

  public EnvoyTokenAuthFilter(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String tokenValue;
    try {
      tokenValue = extractToken(request);
    } catch (AuthenticationException e) {
      handleAuthenticationFailure(request, response, e);
      return;
    }

    if (tokenValue != null) {

      final String tenantId = tokenService.validate(tokenValue);

      if (tenantId == null) {
        SecurityContextHolder.clearContext();
        handleAuthenticationFailure(
            request, response, new BadCredentialsException("Invalid Envoy token"));
        return;
      }
      else {
        log.debug("Authenticated tenant={}", tenantId);
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(
          tenantId, "", AUTHORITIES
        ));
        SecurityContextHolder.setContext(context);
      }
    }

    filterChain.doFilter(request, response);
  }

  private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                           AuthenticationException e)
      throws IOException, ServletException {
    log.debug("Failed to authenticate request from remoteAddr={}", request.getRemoteAddr());
    failureHandler.onAuthenticationFailure(request, response, e);
  }

  private String extractToken(HttpServletRequest request) {
    final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
      Matcher matcher = authorizationPattern.matcher(authorization);

      if (!matcher.matches()) {
        throw new BadCredentialsException("Bearer token is malformed");
      }

      return matcher.group("token");
    }
    else {
      return null;
    }
  }

  public AuthenticationFailureHandler getFailureHandler() {
    return failureHandler;
  }

  public void setFailureHandler(
      AuthenticationFailureHandler failureHandler) {
    this.failureHandler = failureHandler;
  }
}
