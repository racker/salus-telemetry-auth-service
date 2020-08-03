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

import com.rackspace.salus.authservice.config.CacheConfig;
import com.rackspace.salus.telemetry.entities.EnvoyToken;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.repositories.EnvoyTokenRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class TokenService {

  private static final String MSG_NOT_FOUND = "Could not find given token for the tenant";

  private final EnvoyTokenRepository repository;
  private final TokenGenerator tokenGenerator;
  private final Cache tokenCache;
  private final Counter validTokenValidations;
  private final Counter invalidTokenValidations;

  @Autowired
  public TokenService(CacheManager cacheManager, MeterRegistry meterRegistry,
                      EnvoyTokenRepository repository, TokenGenerator tokenGenerator) {
    this.tokenCache = cacheManager.getCache(CacheConfig.TOKEN_VALIDATION);
    Assert.state(tokenCache!=null, "Unable to locate token validation cache");
    this.repository = repository;
    this.tokenGenerator = tokenGenerator;

    validTokenValidations = meterRegistry.counter("tokenValidations", "result", "valid");
    invalidTokenValidations = meterRegistry.counter("tokenValidations", "result", "invalid");
  }

  public EnvoyToken allocate(String tenantId, String description) {
    final EnvoyToken envoyToken = new EnvoyToken()
        .setToken(tokenGenerator.generate())
        .setTenantId(tenantId)
        .setDescription(description);

    return repository.save(envoyToken);
  }

  /**
   * @param tokenValue the tokenValue value to validate
   * @return the tenantId of the validated token or null if given token value is not valid
   */
  @Cacheable(cacheNames = CacheConfig.TOKEN_VALIDATION,
      // avoid cache bloat from a brute force attack
      unless = "#result == null")
  public String validate(String tokenValue) {
    final Optional<EnvoyToken> token = repository.findByToken(tokenValue);
    if (token.isEmpty()) {
      invalidTokenValidations.increment();
      return null;
    }
    validTokenValidations.increment();

    token.get().setLastUsed(Instant.now());
    repository.save(token.get());

    return token.get().getTenantId();
  }

  public EnvoyToken getOne(String tenantId, UUID tokenId) {
    final Optional<EnvoyToken> token = repository
        .findByIdAndTenantId(tokenId, tenantId);

    return token
        .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));
  }

  public Page<EnvoyToken> getAll(String tenantId, Pageable page) {
    return repository.findByTenantId(tenantId, page);
  }

  public EnvoyToken update(String tenantId, UUID tokenId, String description) {
    final EnvoyToken token = repository
        .findByIdAndTenantId(tokenId, tenantId)
        .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));

    token.setDescription(description);

    return repository.save(token);
  }

  public void delete(String tenantId, UUID tokenId) {
    final EnvoyToken token = repository
        .findByIdAndTenantId(tokenId, tenantId)
        .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));

    // Unable to use declarative cache eviction since the cache key is not available
    // until after retrieval of the EnvoyToken
    tokenCache.evict(token.getToken());

    repository.delete(token);
  }

  public void deleteAllForTenant(String tenantId) {
    repository.findByTenantId(tenantId, Pageable.unpaged())
        .forEach(token -> tokenCache.evict(token.getToken()));

    repository.deleteAllByTenantId(tenantId);
  }
}
