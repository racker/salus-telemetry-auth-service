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

import com.rackspace.salus.telemetry.entities.EnvoyToken;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.repositories.EnvoyTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private static final String MSG_NOT_FOUND = "Could not find given token for the tenant";

  private final EnvoyTokenRepository repository;
  private final TokenGenerator tokenGenerator;

  @Autowired
  public TokenService(EnvoyTokenRepository repository, TokenGenerator tokenGenerator) {
    this.repository = repository;
    this.tokenGenerator = tokenGenerator;
  }

  public EnvoyToken allocate(String tenantId, String description) {
    final EnvoyToken envoyToken = new EnvoyToken()
        .setToken(tokenGenerator.generate())
        .setTenantId(tenantId)
        .setDescription(description);

    return repository.save(envoyToken);
  }

  /**
   *
   * @param tokenValue the tokenValue value to validate
   * @return the tenantId of the validated token or null if given token value is not valid
   */
  public String validate(String tokenValue) {
    final Optional<EnvoyToken> token = repository.findByToken(tokenValue);
    if (token.isEmpty()) {
      return null;
    }

    token.get().setLastUsed(Instant.now());
    repository.save(token.get());

    return token.get().getTenantId();
  }

  public EnvoyToken getOne(String tenantId, String tokenValue) {
    final Optional<EnvoyToken> token = repository
        .findByTenantIdAndToken(tenantId, tokenValue);

    return token
        .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));
  }

  public List<EnvoyToken> getAll(String tenantId) {
    return repository.findByTenantId(tenantId);
  }

  public EnvoyToken modify(String tenantId, String tokenValue, String description) {
    final EnvoyToken token = repository
        .findByTenantIdAndToken(tenantId, tokenValue)
        .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));

    token.setDescription(description);

    return repository.save(token);
  }

  public void delete(String tenantId, String tokenValue) {
    final EnvoyToken token = repository
        .findByTenantIdAndToken(tenantId, tokenValue)
        .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));

    repository.delete(token);
  }
}
