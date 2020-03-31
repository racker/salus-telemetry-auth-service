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

package com.rackspace.salus.authservice.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

@ConfigurationProperties("salus.auth.cache")
@Component
@Data
public class CacheProperties {

  SizeAndTtl certs =
      new SizeAndTtl();

  SizeAndTtl tokenValidation =
      new SizeAndTtl().setTtl(Duration.ofSeconds(60));

  @Data
  public static class SizeAndTtl {
    int maxSize = 500;
    @DurationUnit(ChronoUnit.SECONDS)
    Duration ttl = Duration.ofSeconds(600);
  }
}
