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

/**
 * Marker of the internal failures. The message is free text for logs and is never a response code:
 * the public code is the fixed {@code internalError} and the status is 500.
 */
public sealed interface TechnicalException
    permits CryptoException,
        EventResponseException,
        JsonParseException,
        ErrorOccurredException,
        ManifestListResolutionException,
        ManifestParseException,
        ManifestSerializationException,
        SslContextInitializationException {}
