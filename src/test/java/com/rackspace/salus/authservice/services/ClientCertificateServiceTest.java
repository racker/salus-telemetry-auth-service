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

package com.rackspace.salus.authservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rackspace.salus.authservice.config.AuthProperties;
import com.rackspace.salus.authservice.config.CacheConfig;
import com.rackspace.salus.authservice.config.CacheProperties;
import com.rackspace.salus.authservice.web.CertResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.vault.core.VaultPkiOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateResponse;

@RunWith(SpringRunner.class)
@SpringBootTest(
    properties = {
        // explicitly configure cache to hold more than the one tenant tested and no TTL
        "salus.auth.cache.certs.max-size=10",
        "salus.auth.service.pki-role-name=testing-role"
    },
    classes = {
        CacheConfig.class,
        ClientCertificateService.class
    }
)
@AutoConfigureCache(cacheProvider = CacheType.JCACHE)
@Import({SimpleMeterRegistry.class})
@EnableConfigurationProperties({AuthProperties.class, CacheProperties.class})
public class ClientCertificateServiceTest {

  @Autowired
  ClientCertificateService clientCertificateService;

  @MockBean
  VaultTemplate vt;
  @Mock
  VaultPkiOperations pki;
  @Mock
  VaultCertificateResponse cr;
  @Mock
  CertificateBundle cb;

  @Test
  public void testGetClientCertificate() {
    when(vt.opsForPki()).thenReturn(pki);
    when(pki.issueCertificate(any(), any())).thenReturn(cr);
    when(cr.getData()).thenReturn(cb);
    when(cb.getCertificate()).thenReturn("cert");
    when(cb.getIssuingCaCertificate()).thenReturn("ica");
    when(cb.getPrivateKey()).thenReturn("key");

    final CertResponse expectedResp = new CertResponse(
        "-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----",
        "-----BEGIN CERTIFICATE-----\nica\n-----END CERTIFICATE-----",
        "-----BEGIN RSA PRIVATE KEY-----\nkey\n-----END RSA PRIVATE KEY-----"
    );

    final String tenant = String.format("t-%04d", new Random().nextInt(9999));

    final CertResponse resp = clientCertificateService.getClientCertificate(tenant);
    assertThat(resp).isEqualTo(expectedResp);

    // this call should get intercepted by cache
    final CertResponse cachedResp = clientCertificateService.getClientCertificate(tenant);
    assertThat(cachedResp).isEqualTo(expectedResp);

    // ...so underlying Vault invocation count should only be 1
    verify(pki, times(1)).issueCertificate(eq("testing-role"), argThat(req -> {
      assertThat(req.getCommonName()).isEqualTo(tenant);
      return true;
    }));
  }
}