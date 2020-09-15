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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.authservice.services.TokenService;
import com.rackspace.salus.telemetry.entities.EnvoyToken;
import com.rackspace.salus.telemetry.repositories.TenantMetadataRepository;
import com.rackspace.salus.telemetry.web.TenantVerification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@WebMvcTest(EnvoyTokenController.class)
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
public class EnvoyTokenControllerTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  TokenService tokenService;

  @MockBean
  TenantMetadataRepository tenantMetadataRepository;

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Test
  public void testTenantVerification_Success() throws Exception {
    final String tenantId = randomAlphanumeric(10);
    final UUID tokenId = UUID.randomUUID();

    when(tenantMetadataRepository.existsByTenantId(tenantId))
        .thenReturn(true);

    mvc.perform(
        delete("/api/tenant/{tenantId}/envoy-tokens/{id}", tenantId, tokenId)
        // header must be set to trigger tenant verification
        .header(TenantVerification.HEADER_TENANT, tenantId))
        .andExpect(status().isNoContent());

    verify(tenantMetadataRepository).existsByTenantId(tenantId);
  }

  @Test
  public void testTenantVerification_Fail() throws Exception {
    final String tenantId = randomAlphanumeric(10);
    final UUID tokenId = UUID.randomUUID();

    when(tenantMetadataRepository.existsByTenantId(tenantId))
        .thenReturn(false);

    mvc.perform(
        delete("/api/tenant/{tenantId}/envoy-tokens/{id}", tenantId, tokenId)
        // header must be set to trigger tenant verification
        .header(TenantVerification.HEADER_TENANT, tenantId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message", is(TenantVerification.ERROR_MSG)));

    verify(tenantMetadataRepository).existsByTenantId(tenantId);
  }

  @Test
  public void testAllocate() throws Exception {
    final EnvoyToken token = podamFactory.manufacturePojo(EnvoyToken.class);

    when(tokenService.allocate(any(), any()))
        .thenReturn(token);

    final Map<String, String> request = Map.of("description", token.getDescription());

    mvc.perform(
        post("/api/tenant/{tenantId}/envoy-tokens", token.getTenantId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    )
        .andExpect(status().isCreated())
    .andExpect(jsonPath("$.token").value(token.getToken()));

    verify(tokenService).allocate(token.getTenantId(), token.getDescription());
  }

  @Test
  public void testGetAll() throws Exception {
    final String tenantId = randomAlphanumeric(10);

    final List<EnvoyToken> tokens = IntStream.range(0, 10).mapToObj(value ->
        podamFactory.manufacturePojo(EnvoyToken.class)
            .setTenantId(tenantId)
    )
        .collect(Collectors.toList());

    when(tokenService.getAll(any(), any()))
        .thenReturn(new PageImpl<>(tokens));

    mvc.perform(
        get("/api/tenant/{tenantId}/envoy-tokens?size=10", tenantId)
            .accept(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isOk())
        // spot check some response content
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.content.length()").value(10))
        .andExpect(jsonPath("$.content[0].tenantId").value(tenantId))
        .andExpect(jsonPath("$.content[0].id").value(tokens.get(0).getId().toString()))
        .andExpect(jsonPath("$.content[0].token").value(tokens.get(0).getToken()));

    verify(tokenService).getAll(tenantId, PageRequest.of(0, 10));
  }

  @Test
  public void testGetOne() throws Exception {
    final EnvoyToken token = podamFactory.manufacturePojo(EnvoyToken.class);

    when(tokenService.getOne(any(), any()))
        .thenReturn(token);

    mvc.perform(
        get("/api/tenant/{tenantId}/envoy-tokens/{id}",
            token.getTenantId(), token.getId())
        .accept(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(token.getToken()));

    verify(tokenService).getOne(token.getTenantId(), token.getId());
  }

  @Test
  public void testUpdate() throws Exception {
    final EnvoyToken token = podamFactory.manufacturePojo(EnvoyToken.class);

    when(tokenService.update(any(), any(), any()))
        .thenReturn(token);

    final Map<String, String> request = Map.of("description", token.getDescription());

    mvc.perform(
        put("/api/tenant/{tenantId}/envoy-tokens/{id}",
            token.getTenantId(), token.getId()
        )
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .accept(MediaType.APPLICATION_JSON)
    )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value(token.getDescription()));

    verify(tokenService).update(
        token.getTenantId(), token.getId(), token.getDescription()
    );
  }

  @Test
  public void testDelete() throws Exception {
    final String tenantId = randomAlphanumeric(10);
    final UUID tokenId = UUID.randomUUID();

    mvc.perform(
        delete("/api/tenant/{tenantId}/envoy-tokens/{id}", tenantId, tokenId)
    )
        .andExpect(status().isNoContent());

    verify(tokenService).delete(tenantId, tokenId);
  }
}