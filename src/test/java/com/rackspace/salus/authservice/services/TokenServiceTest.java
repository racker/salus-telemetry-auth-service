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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rackspace.salus.telemetry.entities.EnvoyToken;
import com.rackspace.salus.telemetry.model.NotFoundException;
import com.rackspace.salus.telemetry.repositories.EnvoyTokenRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TokenService.class)
public class TokenServiceTest {

  @MockBean
  EnvoyTokenRepository envoyTokenRepository;

  @MockBean
  TokenGenerator tokenGenerator;

  @Autowired
  TokenService tokenService;

  private final PodamFactory podamFactory = new PodamFactoryImpl();

  @Test
  public void testAllocate() {
    final String tenantId = randomAlphanumeric(10);
    final String description = randomAlphanumeric(50);
    final String tokenValue = randomAlphanumeric(24);

    when(envoyTokenRepository.save(any()))
        // return the given entity
        .then(invocationOnMock -> invocationOnMock.getArgument(0));

    when(tokenGenerator.generate())
        .thenReturn(tokenValue);

    final EnvoyToken token = tokenService
        .allocate(tenantId, description);

    assertThat(token).isEqualToIgnoringGivenFields(
        new EnvoyToken()
            .setTenantId(tenantId)
            .setToken(tokenValue)
            .setDescription(description),
        "createdTimestamp"
    );

    verify(tokenGenerator).generate();

    verify(envoyTokenRepository).save(argThat(arg -> {
      assertThat(arg).isEqualToIgnoringGivenFields(
          new EnvoyToken()
              .setTenantId(tenantId)
              .setToken(tokenValue)
              .setDescription(description),
          "createdTimestamp"
      );
      return true;
    }));

  }

  @Test
  public void testValidate_exists() {
    final EnvoyToken envoyToken = podamFactory.manufacturePojo(EnvoyToken.class);

    when(envoyTokenRepository.findByToken(any()))
        .thenReturn(Optional.of(envoyToken));

    final String tokenValue = envoyToken.getToken();

    final String tenantId = tokenService.validate(tokenValue);

    assertThat(tenantId).isEqualTo(envoyToken.getTenantId());

    verify(envoyTokenRepository).findByToken(tokenValue);
  }

  @Test
  public void testValidate_absent() {
    final String tokenValue = randomAlphanumeric(24);

    when(envoyTokenRepository.findByToken(any()))
        .thenReturn(Optional.empty());

    final String tenantId = tokenService.validate(tokenValue);

    assertThat(tenantId).isNull();

    verify(envoyTokenRepository).findByToken(tokenValue);
  }

  @Test
  public void testGetOne_exists() {
    final EnvoyToken envoyToken = podamFactory.manufacturePojo(EnvoyToken.class);

    when(envoyTokenRepository.findByTenantIdAndToken(any(), any()))
        .thenReturn(Optional.of(envoyToken));

    final EnvoyToken result = tokenService.getOne(envoyToken.getTenantId(), envoyToken.getToken());

    assertThat(result).isSameAs(envoyToken);

    verify(envoyTokenRepository)
        .findByTenantIdAndToken(envoyToken.getTenantId(), envoyToken.getToken());
  }

  @Test
  public void testGetOne_absent() {
    final String tenantId = randomAlphanumeric(10);
    final String tokenValue = randomAlphanumeric(24);

    when(envoyTokenRepository.findByTenantIdAndToken(any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> {
      tokenService.getOne(tenantId, tokenValue);
    }).isInstanceOf(NotFoundException.class);

    verify(envoyTokenRepository)
        .findByTenantIdAndToken(tenantId, tokenValue);
  }

  @Test
  public void testGetAll() {
    final String tenantId = randomAlphanumeric(10);

    final List<EnvoyToken> tokens = IntStream.range(0, 10).mapToObj(value ->
        podamFactory.manufacturePojo(EnvoyToken.class)
            .setTenantId(tenantId)
    )
        .collect(Collectors.toList());

    when(envoyTokenRepository.findByTenantId(any(), any()))
        .thenReturn(new PageImpl<>(tokens));

    final Page<EnvoyToken> result = tokenService.getAll(tenantId, PageRequest.of(0, 10));

    assertThat(result.getContent()).isEqualTo(tokens);

    verify(envoyTokenRepository).findByTenantId(tenantId, PageRequest.of(0,10));
  }

  @Test
  public void testModify_exists() {
    final EnvoyToken envoyToken = podamFactory.manufacturePojo(EnvoyToken.class);
    final String tenantId = envoyToken.getTenantId();
    final String newDescription = randomAlphanumeric(50);

    when(envoyTokenRepository.save(any()))
        // return the given entity
        .then(invocationOnMock -> invocationOnMock.getArgument(0));

    when(envoyTokenRepository.findByTenantIdAndToken(any(), any()))
        .thenReturn(Optional.of(envoyToken));

    final EnvoyToken result = tokenService.modify(tenantId, envoyToken.getToken(), newDescription);

    final EnvoyToken expected = new EnvoyToken()
        .setTenantId(tenantId)
        .setToken(envoyToken.getToken())
        // should change description
        .setDescription(newDescription)
        // should leave timestamps as-is
        .setCreatedTimestamp(envoyToken.getCreatedTimestamp())
        // and ignore updatedTimestamp since mock isn't touching it
        .setUpdatedTimestamp(envoyToken.getUpdatedTimestamp())
        .setLastUsed(envoyToken.getLastUsed());

    assertThat(result).isEqualTo(expected);

    verify(envoyTokenRepository).findByTenantIdAndToken(tenantId, envoyToken.getToken());

    verify(envoyTokenRepository).save(expected);
  }

  @Test
  public void testModify_absent() {
    final String tenantId = randomAlphanumeric(10);
    final String tokenValue = randomAlphanumeric(24);

    when(envoyTokenRepository.findByTenantIdAndToken(any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> {
      tokenService.modify(tenantId, tokenValue, "won't be used");
    }).isInstanceOf(NotFoundException.class);

    verify(envoyTokenRepository).findByTenantIdAndToken(tenantId, tokenValue);
    verify(envoyTokenRepository, never()).save(any());
  }

  @Test
  public void testDelete_exists() {
    final EnvoyToken envoyToken = podamFactory.manufacturePojo(EnvoyToken.class);

    when(envoyTokenRepository.findByTenantIdAndToken(any(), any()))
        .thenReturn(Optional.of(envoyToken));

    tokenService.delete(envoyToken.getTenantId(), envoyToken.getToken());

    verify(envoyTokenRepository)
        .findByTenantIdAndToken(envoyToken.getTenantId(), envoyToken.getToken());

    verify(envoyTokenRepository).delete(envoyToken);
  }

  @Test
  public void testDelete_absent() {
    final String tenantId = randomAlphanumeric(10);
    final String tokenValue = randomAlphanumeric(24);

    when(envoyTokenRepository.findByTenantIdAndToken(any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> {
      tokenService.delete(tenantId, tokenValue);
    }).isInstanceOf(NotFoundException.class);

    verify(envoyTokenRepository).findByTenantIdAndToken(tenantId, tokenValue);
    verify(envoyTokenRepository, never()).delete(any());
  }
}