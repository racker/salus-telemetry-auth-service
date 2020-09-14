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

import com.rackspace.salus.common.config.MetricNames;
import com.rackspace.salus.common.config.MetricTags;
import com.rackspace.salus.common.web.AbstractRestExceptionHandler;
import com.rackspace.salus.telemetry.model.NotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

@ControllerAdvice(basePackages = "com.rackspace.salus.authservice.web")
@ResponseBody
public class RestExceptionHandler extends AbstractRestExceptionHandler {

  MeterRegistry meterRegistry;
  private final Counter.Builder authServiceErrorCounter;

  @Autowired
  public RestExceptionHandler(ErrorAttributes errorAttributes, MeterRegistry meterRegistry) {
    super(errorAttributes);
    this.meterRegistry = meterRegistry;
    authServiceErrorCounter = Counter.builder(MetricNames.SERVICE_OPERATION_FAILED);
  }

  @ExceptionHandler({NotFoundException.class})
  public ResponseEntity<?> handleNotFound(
      HttpServletRequest request, Exception e) {
    authServiceErrorCounter.tags(
        MetricTags.URI_METRIC_TAG,request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(), MetricTags.EXCEPTION_METRIC_TAG,e.getClass().getSimpleName())
        .register(meterRegistry).increment();
    return respondWith(request, HttpStatus.NOT_FOUND);
  }
}
