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
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class EnvoyTokenAuthFilter extends OncePerRequestFilter {

  public static final String ROLE_CERT_REQUESTOR = "CERT_REQUESTOR";

  public static final GrantedAuthority CERT_REQUESTOR =
      new SimpleGrantedAuthority(ROLE_CERT_REQUESTOR);

  private static final Collection<? extends GrantedAuthority> AUTHORITIES = List.of(CERT_REQUESTOR);
  private final TokenService tokenService;

  public EnvoyTokenAuthFilter(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.startsWithIgnoreCase(authorization, "bearer ")) {
      final String tokenValue = authorization.substring(7);

      final String tenantId = tokenService.validate(tokenValue);

      if (tenantId == null) {
        SecurityContextHolder.clearContext();
      }
      else {
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(
          tenantId, "", AUTHORITIES
        ));
        SecurityContextHolder.setContext(context);
      }
    }

    filterChain.doFilter(request, response);
  }
}
