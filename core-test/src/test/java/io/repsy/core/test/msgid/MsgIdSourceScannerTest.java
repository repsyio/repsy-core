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
package io.repsy.core.test.msgid;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("MsgIdSourceScanner")
class MsgIdSourceScannerTest {

  private final MsgIdSourceScanner scanner = MsgIdSourceScanner.builder().build();

  private Set<String> used(final String source) {
    return scanner.scan(Map.of("A.java", source), Map.of()).usedMsgIds().keySet();
  }

  @Test
  @DisplayName("finds a literal and a constant on a response factory call")
  void findsResponseFactoryMsgIds() {
    final var scan =
        scanner.scan(
            Map.of(
                "A.java",
                """
                resp.success("loginSucceeded", data);
                this.responseFactory.warning(WARN_ID, x);
                restResponseFactory.error(Codes.NOT_FOUND)
                """),
            Map.of("WARN_ID", Set.of("careful"), "NOT_FOUND", Set.of("notFound")));

    assertThat(scan.usedMsgIds()).containsOnlyKeys("loginSucceeded", "careful", "notFound");
    assertThat(scan.usedMsgIds().get("careful")).containsExactly("A.java");
    assertThat(scan.unresolvedConstants()).isEmpty();
  }

  @Test
  @DisplayName("finds every default exception type")
  void findsEveryDefaultExceptionType() {
    for (final var prefix : MsgIdSourceScanner.DEFAULT_EXCEPTION_PREFIXES) {
      assertThat(used("throw new " + prefix + "Exception(\"someId\");")).containsExactly("someId");
    }
  }

  @Test
  @DisplayName("ignores other calls, other exceptions and non-identifier arguments")
  void ignoresUnrelatedCalls() {
    assertThat(
            used(
                """
                log.error("notAMsgId");
                throw new IllegalStateException("alsoNot");
                throw new BadRequestException(errorKey);
                throw new BadRequestException("has space", x);
                other.success("nope");
                """))
        .isEmpty();
  }

  @Test
  @DisplayName("reports a constant no scanned class defines, with the file")
  void reportsUnresolvedConstants() {
    final var scan =
        scanner.scan(Map.of("B.java", "throw new MfaException(ErrorConstants.MISSING);"), Map.of());

    assertThat(scan.usedMsgIds()).isEmpty();
    assertThat(scan.unresolvedConstants()).containsExactly("ErrorConstants.MISSING (B.java)");
  }

  @Test
  @DisplayName("resolves a name that several classes define to every value")
  void collectsConstantsOfSeveralClasses() {
    final var constants =
        MsgIdSourceScanner.collectConstants(
            List.of(
                "public static final String A_ID = \"first\";",
                "static final @Deprecated String A_ID = \"second\";"),
            List.of("static final String B_ID = \"third\";", "static final int N = 3;"));

    assertThat(constants).containsOnlyKeys("A_ID", "B_ID");
    assertThat(constants.get("A_ID")).containsExactlyInAnyOrder("first", "second");
  }

  @Test
  @DisplayName("flags free text, concatenation, .formatted( and String.format(")
  void flagsNonIdentifiers() {
    assertThat(
            scanner.findNonIdentifierMsgIds(
                """
                throw new BadRequestException("Invalid path");
                throw new ItemNotFoundException("no key with Id %s".formatted(id));
                throw new ItemAlreadyExistException("chartAlreadyExists: " + name);
                throw new ItemAlreadyExistException("chartAlreadyExists"
                    + ":" + name);
                throw new AccessNotAllowedException("quote \\" inside", cause);
                throw new MfaException("mfa %s" .formatted(user));
                throw new BadRequestException(String.format("Invalid %s", type));
                throw new SubscriptionLimitReachedException(MessageFormat.format("x {0}", a));
                throw new DataExportRequestException(String
                    .format("x %s", a));
                """))
        .containsExactly(
            "Invalid path",
            "no key with Id %s",
            "chartAlreadyExists: ",
            "chartAlreadyExists",
            "quote \\\" inside",
            "mfa %s",
            "String.format(",
            "MessageFormat.format(",
            "String.format(");
  }

  @Test
  @DisplayName("leaves fixed identifiers, constants and other arguments alone")
  void ignoresFixedIdentifiers() {
    assertThat(
            scanner.findNonIdentifierMsgIds(
                """
                throw new BadRequestException("chartNameMissing");
                throw new ItemNotFoundException(ERR_NOT_FOUND);
                throw new UnAuthorizedException("unAuthorized", Map.of("k", "v"));
                throw new ErrorOccurredException(e);
                throw new IllegalArgumentException("free text is fine here " + name);
                log.warn("no key with Id {}".formatted(id));
                """))
        .isEmpty();
  }

  @Test
  @DisplayName("flags free text on every default exception type, so a type cannot drop out")
  void flagsEveryDefaultExceptionType() {
    final var source =
        String.join(
            "\n",
            MsgIdSourceScanner.DEFAULT_EXCEPTION_PREFIXES.stream()
                .map(type -> "throw new " + type + "Exception(\"free text\");")
                .toList());

    assertThat(scanner.findNonIdentifierMsgIds(source))
        .hasSize(MsgIdSourceScanner.DEFAULT_EXCEPTION_PREFIXES.size());
  }

  @Test
  @DisplayName("reports the non-identifier msgIds with their files")
  void scanReportsNonIdentifierFiles() {
    final var scan =
        scanner.scan(Map.of("C.java", "throw new BadRequestException(\"Free text\");"), Map.of());

    assertThat(scan.nonIdentifierMsgIds()).containsOnlyKeys("Free text");
    assertThat(scan.nonIdentifierMsgIds().get("Free text")).containsExactly("C.java");
  }

  @Test
  @DisplayName("takes its exception types and response receivers from the builder")
  void honoursBuilderSettings() {
    final var custom =
        MsgIdSourceScanner.builder()
            .exceptionPrefixes(List.of("Teapot"))
            .responseReceivers(List.of("answer"))
            .build();
    final var source =
        """
        throw new TeapotException("brewFailed");
        throw new BadRequestException("ignoredNow");
        answer.error("answered");
        resp.error("ignoredToo");
        """;

    assertThat(custom.scan(Map.of("D.java", source), Map.of()).usedMsgIds())
        .containsOnlyKeys("brewFailed", "answered");
    assertThat(custom.findNonIdentifierMsgIds("throw new TeapotException(\"free text\");"))
        .containsExactly("free text");
    assertThat(custom.findNonIdentifierMsgIds("throw new BadRequestException(\"free text\");"))
        .isEmpty();
  }

  @Test
  @DisplayName("scans the product's own call shapes added as extra patterns")
  void scansExtraPatterns() {
    final var custom =
        MsgIdSourceScanner.builder()
            .extraPattern(
                Pattern.compile(
                    "\\bhandler\\.fail\\(\\s*request\\s*,\\s*[\\w.]+\\s*,\\s*"
                        + MsgIdSourceScanner.LITERAL_OR_CONSTANT))
            .extraPattern(
                Pattern.compile("\\?\\s*\\w+\\s*:\\s*(?<constant>Codes\\.[A-Z][A-Z0-9_]*)\\s*;"))
            .build();
    final var source =
        """
        handler.fail(request, HttpStatus.GONE, "movedAway");
        var id = message != null ? message : Codes.FALLBACK;
        """;

    assertThat(
            custom
                .scan(Map.of("E.java", source), Map.of("FALLBACK", Set.of("fellBack")))
                .usedMsgIds())
        .containsOnlyKeys("movedAway", "fellBack");
  }

  @Test
  @DisplayName("reads the sources under every source root and finds the roots")
  void readsSources(@TempDir final Path dir) throws IOException {
    final var main = dir.resolve("mod-a/src/main/java/p");
    final var test = dir.resolve("mod-a/src/test/java/p");

    Files.createDirectories(main);
    Files.createDirectories(test);
    Files.writeString(main.resolve("X.java"), "class X {}");
    Files.writeString(main.resolve("notes.txt"), "not java");
    Files.writeString(test.resolve("XTest.java"), "class XTest {}");

    final var roots = MsgIdSourceScanner.findSourceRoots(dir, 6);

    assertThat(roots).containsExactly(dir.resolve("mod-a/src/main/java"));
    assertThat(MsgIdSourceScanner.readSources(roots))
        .containsOnlyKeys(Path.of("p", "X.java").toString());
    assertThat(MsgIdSourceScanner.findSourceRoots(dir.resolve("mod-a/src/test"), 6)).isEmpty();
  }
}
