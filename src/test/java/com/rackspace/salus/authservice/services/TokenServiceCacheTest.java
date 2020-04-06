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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rackspace.salus.authservice.config.CacheConfig;
import com.rackspace.salus.authservice.config.CacheProperties;
import com.rackspace.salus.telemetry.entities.EnvoyToken;
import com.rackspace.salus.telemetry.repositories.EnvoyTokenRepository;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    // for real jcache initialization
    CacheConfig.class,
    MeterRegistryConfig.class,
    TokenService.class,
})
@AutoConfigureCache(cacheProvider = CacheType.JCACHE)
@EnableConfigurationProperties({CacheProperties.class})
public class TokenServiceCacheTest {

  @MockBean
  EnvoyTokenRepository envoyTokenRepository;

  @MockBean
  TokenGenerator tokenGenerator;

  @Autowired
  TokenService tokenService;

  private final PodamFactory podamFactory = new PodamFactoryImpl();

  @Test
  public void testCachingViaValidate() {
    final EnvoyToken token = podamFactory.manufacturePojo(EnvoyToken.class);

    when(envoyTokenRepository.findByToken(any()))
        .thenReturn(Optional.of(token));

    final String tenantId = tokenService.validate(token.getToken());
    assertThat(tenantId).isEqualTo(token.getTenantId());

    // call again, should be cached
    final String cachedTenantId = tokenService.validate(token.getToken());
    assertThat(cachedTenantId).isEqualTo(token.getTenantId());

    verify(envoyTokenRepository, times(1))
        .findByToken(token.getToken());
  }

  @Test
  public void testDontCacheInvalidTokens() {
    final String tokenValue = randomAlphanumeric(24);

    when(envoyTokenRepository.findByToken(any()))
        .thenReturn(Optional.empty());

    final String tenantId = tokenService.validate(tokenValue);
    assertThat(tenantId).isNull();

    // call again, should NOT be cached
    final String tenantId2 = tokenService.validate(tokenValue);
    assertThat(tenantId2).isNull();

    verify(envoyTokenRepository, times(2))
        .findByToken(tokenValue);
  }

  @Test
  public void testCachingEvictionViaDelete() {
    final EnvoyToken token = podamFactory.manufacturePojo(EnvoyToken.class);

    when(envoyTokenRepository.findByToken(any()))
        // pre-cache call
        .thenReturn(Optional.of(token))
        // post-delete call
        .thenReturn(Optional.empty());

    // used during delete
    when(envoyTokenRepository.findByIdAndTenantId(any(), any()))
        .thenReturn(Optional.of(token));

    final String tenantId = tokenService.validate(token.getToken());
    assertThat(tenantId).isEqualTo(token.getTenantId());

    // call again, should be cached
    final String cachedTenantId = tokenService.validate(token.getToken());
    assertThat(cachedTenantId).isEqualTo(token.getTenantId());

    tokenService.delete(token.getTenantId(), token.getId());

    final String postDeleteResult = tokenService.validate(token.getToken());
    assertThat(postDeleteResult).isNull();

    // called 1x for first validate and 1x after deletion
    verify(envoyTokenRepository, times(2))
        .findByToken(token.getToken());
  }
}
