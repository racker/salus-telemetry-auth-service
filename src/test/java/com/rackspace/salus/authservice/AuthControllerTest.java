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

package com.rackspace.salus.authservice;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rackspace.salus.authservice.services.ClientCertificateService;
import com.rackspace.salus.authservice.web.CertResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

   @Autowired
   private MockMvc mvc;
   @MockBean
   ClientCertificateService clientCertificateService;

   @Test
   public void getCertSuccessful() throws Exception {

      final CertResponse certResponse = new CertResponse(
          "-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----",
          "-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----",
          "-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----"
      );
      when(clientCertificateService.getClientCertificate(any()))
          .thenReturn(certResponse);

      HttpHeaders h = new HttpHeaders();
      h.add("X-Roles", "compute:default");
      h.add("X-Tenant-Id", "123456");
      mvc.perform(
               get("/v1.0/tenant/123456/auth/cert")
                   .headers(h))
                   .andExpect(status().is(200))
                   .andExpect(jsonPath("$.certificate",
                       is("-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----")))
                   .andExpect(jsonPath("$.issuingCaCertificate",
                       is("-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----")))
                   .andExpect(jsonPath("$.privateKey",
                       is("-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----")));

      verify(clientCertificateService).getClientCertificate("123456");
   }

   @Test
   public void getCertSuccessful_oldPath() throws Exception {

      final CertResponse certResponse = new CertResponse(
          "-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----",
          "-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----",
          "-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----"
      );
      when(clientCertificateService.getClientCertificate(any()))
          .thenReturn(certResponse);

      HttpHeaders h = new HttpHeaders();
      h.add("X-Roles", "compute:default");
      h.add("X-Tenant-Id", "123456");
      mvc.perform(
               get("/auth/cert")
                   .headers(h))
                   .andExpect(status().is(200))
                   .andExpect(jsonPath("$.certificate",
                       is("-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----")))
                   .andExpect(jsonPath("$.issuingCaCertificate",
                       is("-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----")))
                   .andExpect(jsonPath("$.privateKey",
                       is("-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----")));

      verify(clientCertificateService).getClientCertificate("123456");
   }

   @Test
   public void getCertBadRole() throws Exception {
      HttpHeaders h = new HttpHeaders();
      h.add("X-Roles", "compute:not-default");
      h.add("X-Tenant-Id", "123456");
      mvc.perform(
              get("/auth/cert")
                  .headers(h))
                  .andExpect(status().is(403));
   }

   @Test
   public void getCertNoTenant() throws Exception {
      HttpHeaders h = new HttpHeaders();
      h.add("X-Roles", "compute:default");
      mvc.perform(
              get("/auth/cert")
                  .headers(h))
                  .andExpect(status().is(403));
   }
}

