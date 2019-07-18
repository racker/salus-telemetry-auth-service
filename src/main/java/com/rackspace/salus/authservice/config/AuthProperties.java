/*
 *    Copyright 2018 Rackspace US, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package com.rackspace.salus.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("salus.auth.service")
@Component
@Data
public class AuthProperties {

  /**
   * The roles (without "ROLE_" prefix) that are required to allow the envoy to connect to the auth service.
   * Identity roles are translated to this format via {@link com.rackspace.salus.common.web.PreAuthenticatedFilter}.
   *
   * COMPUTE_DEFAULT is what is used in tests.
   */
  String[] roles = new String[]{"COMPUTE_DEFAULT"};
}
