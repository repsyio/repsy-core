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
package io.repsy.core.error_handling.exceptions;

/** The HTTP status codes of the exceptions, as plain numbers (this module has no Spring web). */
final class Statuses {
  static final int BAD_REQUEST = 400;
  static final int UNAUTHORIZED = 401;
  static final int FORBIDDEN = 403;
  static final int NOT_FOUND = 404;
  static final int CONFLICT = 409;
  static final int INTERNAL_SERVER_ERROR = 500;

  /** The public code of every technical exception. */
  static final String INTERNAL_ERROR_CODE = "internalError";

  private Statuses() {}
}
