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

import static org.assertj.core.api.Assertions.assertThat;

import com.rackspace.salus.authservice.config.AuthProperties;
import java.util.Random;
import org.junit.Test;

public class TokenGeneratorTest {

  @Test
  public void testGenerateToken() {
    final TokenGenerator tokenGenerator = new TokenGenerator(new Random(0), new AuthProperties()
        .setTokenSize(18));

    final String token1 = tokenGenerator.generate();
    assertThat(token1).hasSize(24);

    final String token2 = tokenGenerator.generate();
    assertThat(token2).hasSize(24);

    assertThat(token2).isNotEqualTo(token1);
  }
}