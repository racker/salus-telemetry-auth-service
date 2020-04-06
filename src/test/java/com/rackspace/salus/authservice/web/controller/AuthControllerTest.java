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

package com.rackspace.salus.authservice.web.controller;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rackspace.salus.authservice.services.ClientCertificateService;
import com.rackspace.salus.authservice.services.TokenService;
import com.rackspace.salus.authservice.web.CertResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

  @TestConfiguration
  static class ExtraTestConfig {

    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Autowired
  private MockMvc mvc;

  @MockBean
  ClientCertificateService clientCertificateService;

  @MockBean
  TokenService tokenService;

  @Test
  public void getCertSuccessful() throws Exception {

    final CertResponse certResponse = new CertResponse(
        "-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----",
        "-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----",
        "-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----"
    );
    when(clientCertificateService.getClientCertificate(any()))
        .thenReturn(certResponse);

    final String tenantId = randomAlphanumeric(10);
    final String tokenValue = randomAlphanumeric(24);

    when(tokenService.validate(any()))
        .thenReturn(tenantId);

    mvc.perform(
        get("/v1.0/cert")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue)
    )
        .andExpect(status().is(200))
        .andExpect(jsonPath(
            "$.certificate",
            is("-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----")
        ))
        .andExpect(jsonPath(
            "$.issuingCaCertificate",
            is("-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----")
        ))
        .andExpect(jsonPath(
            "$.privateKey",
            is("-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----")
        ));

    verify(tokenService).validate(tokenValue);

    verify(clientCertificateService).getClientCertificate(tenantId);
  }

  @Test
  public void getCertSuccessful_missingAuth() throws Exception {

    mvc.perform(
        get("/v1.0/cert")
        // don't set headers
    )
        .andExpect(status().isForbidden());

    verify(tokenService, never()).validate(any());

    verify(clientCertificateService, never()).getClientCertificate(any());
  }

  @Test
  public void getCertSuccessful_unknownToken() throws Exception {

    final String tokenValue = randomAlphanumeric(24);

    when(tokenService.validate(any()))
        .thenReturn(null);

    mvc.perform(
        get("/v1.0/cert")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue)
    )
        .andExpect(status().isUnauthorized());

    verify(tokenService).validate(tokenValue);

    verify(clientCertificateService, never()).getClientCertificate(any());
  }

  @Test
  public void getCertSuccessful_malformedToken() throws Exception {

    final String tokenValue = randomAlphanumeric(24);

    when(tokenService.validate(any()))
        .thenReturn(null);

    mvc.perform(
        get("/v1.0/cert")
            .header(HttpHeaders.AUTHORIZATION, "Bearer") // and no token
    )
        .andExpect(status().isUnauthorized());

    verify(tokenService, never()).validate(tokenValue);

    verify(clientCertificateService, never()).getClientCertificate(any());
  }
}

