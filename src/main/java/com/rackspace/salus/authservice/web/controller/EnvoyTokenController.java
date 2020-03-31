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

import com.rackspace.salus.authservice.services.TokenService;
import com.rackspace.salus.authservice.web.model.TokenAllocationRequest;
import com.rackspace.salus.authservice.web.model.TokenModifyRequest;
import com.rackspace.salus.telemetry.entities.EnvoyToken;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is API that is expected to be proxied by the public API
 */
@RestController
@RequestMapping("/api")
@Api(authorizations = {
    @Authorization(value = "repose_auth",
        scopes = {
            @AuthorizationScope(scope = "write:envoy-token", description = "Can allocate envoy-tokens"),
            @AuthorizationScope(scope = "read:envoy-token", description = "Can get and list envoy-tokens"),
            @AuthorizationScope(scope = "delete:envoy-token", description = "Can delete an envoy-token")
        })
})
public class EnvoyTokenController {

  private final TokenService tokenService;

  @Autowired
  public EnvoyTokenController(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @PostMapping("/tenant/{tenantId}/envoy-tokens")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation("Allocate a token to be used by Envoys to retrieve client certificates")
  public EnvoyToken allocate(@PathVariable String tenantId,
                             @RequestBody TokenAllocationRequest request) {
    return tokenService.allocate(tenantId, request.getDescription());
  }

  @GetMapping("/tenant/{tenantId}/envoy-tokens")
  @ApiOperation("Get a page of currently allocated Envoy tokens")
  public Page<EnvoyToken> getAll(@PathVariable String tenantId, Pageable page) {
    return tokenService.getAll(tenantId, page);
  }

  @GetMapping("/tenant/{tenantId}/envoy-tokens/{token}")
  @ApiOperation("Get a specific Envoy token")
  public EnvoyToken getOne(@PathVariable String tenantId,
                           @PathVariable String token) {
    return tokenService.getOne(tenantId, token);
  }

  @PutMapping("/tenant/{tenantId}/envoy-tokens/{token}")
  @ApiOperation("Update fields of a specific Envoy token")
  public EnvoyToken update(@PathVariable String tenantId,
                           @PathVariable String token,
                           @RequestBody TokenModifyRequest request) {
    return tokenService.update(tenantId, token, request.getDescription());
  }

  @DeleteMapping("/tenant/{tenantId}/envoy-tokens/{token}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiOperation("Delete a specific Envoy token")
  public void delete(@PathVariable String tenantId,
                     @PathVariable String token) {
    tokenService.delete(tenantId, token);
  }
}
