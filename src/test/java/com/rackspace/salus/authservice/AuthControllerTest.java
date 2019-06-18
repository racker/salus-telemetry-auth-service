/*
 *    Copyright 2018 Rackspace US, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package com.rackspace.salus.authservice;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.vault.core.VaultPkiOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateResponse;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

   @Autowired
   private MockMvc mvc;
   @MockBean
   VaultTemplate vt;
   @Mock
   VaultPkiOperations pki;
   @Mock
   VaultCertificateResponse cr;
   @Mock
   CertificateBundle cb;

   @Test
   public void getCertSuccessful() throws Exception {

      when(vt.opsForPki()).thenReturn(pki);
      when(pki.issueCertificate(any(), any())).thenReturn(cr);
      when(cr.getData()).thenReturn(cb);
      when(cb.getCertificate()).thenReturn("cert");
      when(cb.getIssuingCaCertificate()).thenReturn("ica");
      when(cb.getPrivateKey()).thenReturn("key");

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
   }

   @Test
   @Ignore
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

