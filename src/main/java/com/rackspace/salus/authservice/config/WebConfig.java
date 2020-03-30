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

package com.rackspace.salus.authservice.config;

import com.rackspace.salus.authservice.services.TokenService;
import com.rackspace.salus.authservice.web.EnvoyTokenAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author Geoff Bourne
 * @since Mar 2017
 */
@Configuration
@Slf4j
public class WebConfig extends WebSecurityConfigurerAdapter {

    private final TokenService tokenService;

    public WebConfig(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.debug("Configuring web security");

        http
            .csrf().disable()
            .addFilterBefore(
                new EnvoyTokenAuthFilter(tokenService),
                BasicAuthenticationFilter.class
            )
            .authorizeRequests()
            .antMatchers("/cert")
            .hasAuthority(EnvoyTokenAuthFilter.ROLE_CERT_REQUESTOR)
            // all other requests are inter-service calls
            .anyRequest().permitAll();
    }
}
