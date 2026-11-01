/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.repsy.core.web_error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jspecify.annotations.Nullable;

/**
 * A request whose URI, URL, servlet path and path info are safe to log (RPS-2174): the
 * secret-bearing segments of the path are masked ({@link UrlMasking#maskPath(String)}). Handed to
 * {@code exceptionToString}, whose report prints the path of the request.
 */
public class MaskedPathRequest extends HttpServletRequestWrapper {

  public MaskedPathRequest(final HttpServletRequest request) {
    super(request);
  }

  /** The request itself when it is already masked. */
  public static HttpServletRequest of(final HttpServletRequest request) {
    return request instanceof MaskedPathRequest ? request : new MaskedPathRequest(request);
  }

  @Override
  public @Nullable String getRequestURI() {
    return UrlMasking.maskPath(super.getRequestURI());
  }

  @Override
  public @Nullable String getServletPath() {
    return UrlMasking.maskPath(super.getServletPath());
  }

  @Override
  public @Nullable String getPathInfo() {
    return UrlMasking.maskPath(super.getPathInfo());
  }

  @Override
  public @Nullable StringBuffer getRequestURL() {
    final var url = super.getRequestURL();

    return url == null ? null : new StringBuffer(UrlMasking.maskPath(url.toString()));
  }
}
