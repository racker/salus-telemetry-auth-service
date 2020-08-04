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

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("salus.auth.service")
@Component
@Data
@Validated
public class AuthProperties {

  /**
   * The roles (without "ROLE_" prefix) that are required to allow the envoy to connect to the auth service.
   * Identity roles are translated to this format via {@link com.rackspace.salus.common.web.PreAuthenticatedFilter}.
   *
   * COMPUTE_DEFAULT is what is used in tests.
   */
  @NotEmpty
  List<String> roles = List.of("COMPUTE_DEFAULT");

  /**
   * The Vault role name provided during PKI certificate issuing requests.
   */
  @NotBlank
  String pkiRoleName = "telemetry-infra";

  /**
   * Amount of random bytes used to feed into token encoding. Choosing a size that is a
   * multiple of 3 is ideal since it avoids the inclusion of Base64 padding.
   */
  @NotNull
  int tokenSize = 18;
}
