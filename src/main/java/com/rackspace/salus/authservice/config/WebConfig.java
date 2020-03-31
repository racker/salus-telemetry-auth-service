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
import com.rackspace.salus.authservice.web.DevTokenAuthFilter;
import com.rackspace.salus.authservice.web.EnvoyTokenAuthFilter;
import javax.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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

    private static final String PROFILE_DEVTOKEN = "devtoken";

    private final TokenService tokenService;
    private final Environment environment;

    public WebConfig(TokenService tokenService, Environment environment) {
        this.tokenService = tokenService;
        this.environment = environment;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.debug("Configuring web security");

        final Filter certAuthFilter;
        // activate on a profile separate from "dev" to allow for testing real token workflow
        if (environment.acceptsProfiles(Profiles.of(PROFILE_DEVTOKEN))) {
            log.warn("Using DevTokenAuthFilter to stub out cert retrieval authentication");
            certAuthFilter = new DevTokenAuthFilter();
        } else {
            certAuthFilter = new EnvoyTokenAuthFilter(tokenService);
        }

        http
            .csrf().disable()
            .addFilterBefore(
                certAuthFilter,
                BasicAuthenticationFilter.class
            )
            .authorizeRequests()
            .antMatchers("/v*/cert")
            .hasAuthority(EnvoyTokenAuthFilter.ROLE_CERT_REQUESTOR)
            // all other requests are proxied via public API, which is already authenticated
            .anyRequest().permitAll();
    }
}
