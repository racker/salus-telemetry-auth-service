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

import com.rackspace.salus.authservice.services.ClientCertificateService;
import com.rackspace.salus.authservice.web.CertResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Api(authorizations = {
    @Authorization("bearer")
})
public class AuthController {

    private final ClientCertificateService clientCertificateService;
    private final Counter certCounter;

    @Autowired
    public AuthController(ClientCertificateService clientCertificateService,
                          MeterRegistry meterRegistry) {
        this.clientCertificateService = clientCertificateService;

        certCounter = meterRegistry.counter("messages","certsAssigned", "stage");
    }

    @GetMapping("/v${salus.api.auth.version}/cert")
    @ApiOperation("Requests client certificate material for the gRPC connection with Ambassador")
    public ResponseEntity<CertResponse> getCertWithTenantFromAuth(
        @AuthenticationPrincipal String tenantId
    ) {
        final CertResponse rd = clientCertificateService.getClientCertificate(tenantId);

        certCounter.increment();
        log.info("Providing client certificates for tenant={}", tenantId);
        return ResponseEntity.ok(rd);
    }
}
