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

package com.rackspace.salus.authservice.web.controller;

import com.rackspace.salus.authservice.web.CertResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final VaultTemplate vaultTemplate;
    private final Counter certCounter;

    @Autowired
    public AuthController(VaultTemplate vaultTemplate,
                          MeterRegistry meterRegistry) {
        this.vaultTemplate = vaultTemplate;

        certCounter = meterRegistry.counter("messages","certsAssigned", "stage");
    }

    @GetMapping("cert")
    // Note Confirm this is the correct role for this method:
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public ResponseEntity<CertResponse> getCert(@RequestHeader("X-Tenant-Id") String tenant) {
        final VaultCertificateResponse resp = vaultTemplate.opsForPki()
                .issueCertificate("telemetry-infra",
                        VaultCertificateRequest.create(tenant));
        CertResponse rd = new CertResponse(formatCert(resp.getData().getCertificate(), "CERTIFICATE"),
                                           formatCert(resp.getData().getIssuingCaCertificate(), "CERTIFICATE"),
                                           formatCert(resp.getData().getPrivateKey(), "RSA PRIVATE KEY"));
        certCounter.increment();
        log.info("Retrieving client certificates for tenant={}", tenant);
        return ResponseEntity.ok(rd);
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
