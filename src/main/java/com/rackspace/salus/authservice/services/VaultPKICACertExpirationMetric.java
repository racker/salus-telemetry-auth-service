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
import com.rackspace.salus.common.config.MetricTags;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;

@Service
@Slf4j
public class VaultPKICACertExpirationMetric implements MeterBinder {

  private VaultTemplate vaultTemplate;
  MeterRegistry meterRegistry;

  @Autowired
  public VaultPKICACertExpirationMetric(VaultTemplate vaultTemplate, AuthProperties properties,
      MeterRegistry meterRegistry) {
    this.vaultTemplate = vaultTemplate;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void bindTo(MeterRegistry meterRegistry) {
    Gauge.builder("envoyAuthenticatingCertExpiration", this, VaultPKICACertExpirationMetric::calculateCACertExpiration)
        .description("Envoy Vault Certificate Expiration Metric")
        .baseUnit("Seconds")
        .tag(MetricTags.SERVICE_METRIC_TAG,"VaultPKICACertExpirationMetric")
        .register(meterRegistry);
  }

  public long calculateCACertExpiration() {
    long diffInSeconds = 0;
    try {
      byte[] bytes = vaultTemplate.doWithSession(restOperations -> restOperations
          .getForObject("/pki/ca/pem", byte[].class));
      X509Certificate myCert = null;
      try {
        myCert = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(
                // string encoded with default charset
                new ByteArrayInputStream(bytes)
            );
      } catch (CertificateException e) {
        log.error("error occurred while calculating CACertExpiration ", e);
      }
      if(myCert != null) {
        diffInSeconds = myCert.getNotAfter().getTime() - System.currentTimeMillis();
      }
    } catch (Exception e) {
      log.error("error occurred reading certificates from vault ", e);
    }
    return diffInSeconds;
  }
}
