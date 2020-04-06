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
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TokenGenerator {

  private final Random tokenRandom;
  private final AuthProperties properties;
  private final Encoder base64Encoder;

  @Autowired
  public TokenGenerator(Random tokenRandom, AuthProperties properties) {
    this.tokenRandom = tokenRandom;
    this.properties = properties;
    this.base64Encoder = Base64.getUrlEncoder();
  }

  public String generate() {
    byte[] bytes = new byte[properties.getTokenSize()];
    tokenRandom.nextBytes(bytes);

    return base64Encoder.encodeToString(bytes);
  }

}
