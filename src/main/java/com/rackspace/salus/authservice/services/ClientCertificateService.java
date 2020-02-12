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

import com.rackspace.salus.authservice.config.AuthProperties;
import com.rackspace.salus.authservice.web.CertResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;

@Service
@Slf4j
public class ClientCertificateService {

  private final VaultTemplate vaultTemplate;
  private final AuthProperties properties;

  @Autowired
  public ClientCertificateService(VaultTemplate vaultTemplate, AuthProperties properties) {
    this.vaultTemplate = vaultTemplate;
    this.properties = properties;
  }

  @Cacheable("clientCerts")
  public CertResponse getClientCertificate(String tenant) {
    log.info("Allocating client certificates for tenant={} from Vault", tenant);

    final VaultCertificateResponse resp = vaultTemplate.opsForPki()
        .issueCertificate(
            properties.getPkiRoleName(),
            VaultCertificateRequest.create(tenant));

    CertResponse rd = new CertResponse(formatCert(resp.getData().getCertificate(), "CERTIFICATE"),
        formatCert(resp.getData().getIssuingCaCertificate(), "CERTIFICATE"),
        formatCert(resp.getData().getPrivateKey(), "RSA PRIVATE KEY"));

    return rd;
  }

  static String formatCert(String c, String name) {
    StringBuilder n = new StringBuilder("-----BEGIN " + name + "-----");
    for (int i = 0; i < c.length() ; i++) {
      if ((i % 64) == 0) {
        n.append("\n");
      }
      n.append(c.charAt(i));
    }
    n.append("\n-----END " + name + "-----");
    return n.toString();
  }
}
