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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

/**
 * Records every log event, at any level, of the classes under test, so a test can prove that a
 * request value (a token, a password, a path segment) never reaches the log (RPS-2174).
 *
 * <p>Register it as a field of the test class: {@code @RegisterExtension final LogLeakRecorder log
 * = LogLeakRecorder.watching(MyAdvice.class);}. It attaches to the loggers of those classes at
 * {@code TRACE} before each test and puts the levels back after it.
 */
public final class LogLeakRecorder implements BeforeEachCallback, AfterEachCallback {

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final List<Logger> loggers;
  private final List<Level> originalLevels = new ArrayList<>();

  private LogLeakRecorder(final List<Logger> loggers) {
    this.loggers = loggers;
  }

  /**
   * Watches the loggers of the given classes.
   *
   * @param sources the classes whose log events are recorded
   * @return the recorder, to be registered as a JUnit extension
   */
  public static LogLeakRecorder watching(final Class<?>... sources) {
    return new LogLeakRecorder(
        Stream.of(sources).map(type -> (Logger) LoggerFactory.getLogger(type)).toList());
  }

  @Override
  public void beforeEach(final ExtensionContext context) {
    this.appender.list.clear();
    this.originalLevels.clear();
    this.appender.start();

    for (final var logger : this.loggers) {
      this.originalLevels.add(logger.getLevel());
      logger.setLevel(Level.TRACE);
      logger.addAppender(this.appender);
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    for (var i = 0; i < this.loggers.size(); i++) {
      this.loggers.get(i).detachAppender(this.appender);
      this.loggers.get(i).setLevel(this.originalLevels.get(i));
    }

    this.appender.stop();
  }

  /**
   * The recorded events.
   *
   * @return the events in the order they were logged
   */
  public List<ILoggingEvent> events() {
    return this.appender.list;
  }

  /**
   * Fails when nothing was logged (the test would prove nothing) or when a message or a stack trace
   * contains {@code secret}.
   *
   * @param secret the value that must not reach the log
   */
  public void assertNothingLogged(final String secret) {
    assertThat(this.appender.list).isNotEmpty();

    for (final var event : this.appender.list) {
      final var text =
          event.getFormattedMessage()
              + (event.getThrowableProxy() != null
                  ? ThrowableProxyUtil.asString(event.getThrowableProxy())
                  : "");

      assertThat(text).doesNotContain(secret);
    }
  }

  /**
   * Fails unless a recorded message contains {@code text}, to prove that the event under test was
   * logged at all and in the masked form.
   *
   * @param text a part of an expected message
   */
  public void assertLoggedMessageContaining(final String text) {
    assertThat(this.appender.list).anyMatch(event -> event.getFormattedMessage().contains(text));
  }
}
