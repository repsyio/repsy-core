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
package io.repsy.core.test.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

class LogLeakRecorderTest {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogLeakRecorderTest.class);

  @RegisterExtension
  final LogLeakRecorder recorder = LogLeakRecorder.watching(LogLeakRecorderTest.class);

  @Test
  void recordsEventsOfEveryLevel() {
    LOG.trace("trace line");
    LOG.error("error line");

    assertThat(this.recorder.events()).hasSize(2);
    this.recorder.assertLoggedMessageContaining("trace line");
  }

  @Test
  void passesWhenNoEventCarriesTheSecret() {
    LOG.info("masked path /npm/-/user/token/***");

    this.recorder.assertNothingLogged("s3cr3t");
    this.recorder.assertLoggedMessageContaining("/npm/-/user/token/***");
  }

  @Test
  void failsWhenAMessageCarriesTheSecret() {
    LOG.info("token s3cr3t");

    assertThatThrownBy(() -> this.recorder.assertNothingLogged("s3cr3t"))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void failsWhenAStackTraceCarriesTheSecret() {
    LOG.warn("failed", new IllegalStateException("for s3cr3t"));

    assertThatThrownBy(() -> this.recorder.assertNothingLogged("s3cr3t"))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void failsWhenNothingWasLoggedBecauseTheTestWouldProveNothing() {
    assertThatThrownBy(() -> this.recorder.assertNothingLogged("s3cr3t"))
        .isInstanceOf(AssertionError.class);
    assertThatThrownBy(() -> this.recorder.assertLoggedMessageContaining("x"))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void putsTheLevelBackAndDetachesAfterTheTest() {
    final var logger = (Logger) LoggerFactory.getLogger(LogLeakRecorderTest.class);
    final var watching = LogLeakRecorder.watching(LogLeakRecorderTest.class);
    logger.setLevel(Level.WARN);

    watching.beforeEach(null);
    assertThat(logger.getLevel()).isEqualTo(Level.TRACE);
    watching.afterEach(null);

    assertThat(logger.getLevel()).isEqualTo(Level.WARN);
    LOG.error("after");
    assertThat(watching.events()).isEmpty();
    logger.setLevel(null);
  }
}
