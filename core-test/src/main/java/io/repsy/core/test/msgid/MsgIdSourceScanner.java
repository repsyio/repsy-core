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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Scans Java sources for the message ids (msgIds) a product's code can put in a response, so that a
 * product test can check each of them against its {@code messages.properties} bundle.
 *
 * <p>The scanner owns the regular expressions, the source walk and the constant resolution that the
 * product tests of Repsy OS, Repsy Cloud and Imentis used to copy. What stays with the product is
 * its own source directories, the bundle, the pending lists and the assertions, so a change here
 * cannot loosen a product's guard without that product's test seeing the new result.
 *
 * <p>What it recognises: an identifier-like string literal or an {@code UPPER_SNAKE} constant that
 * is the first argument of {@code success}/{@code warning}/{@code error} on a response factory, or
 * of the constructor of a msgId-carrying exception. Ids held in other variables are not seen.
 * Product-specific call shapes are added with {@link Builder#extraPattern(Pattern)}. A string
 * literal that starts an exception's msgId but is free text, or continued by a concatenation,
 * {@code .formatted(} or {@code String.format(}, cannot have a bundle entry and is reported by
 * {@link #findNonIdentifierMsgIds(String)}.
 */
public final class MsgIdSourceScanner {

  /**
   * The tail of a pattern that captures a msgId: the named group {@code literal} for a string
   * literal or {@code constant} for an {@code UPPER_SNAKE} constant (optionally qualified),
   * followed by {@code ,} or {@code )}. A pattern given to {@link Builder#extraPattern(Pattern)}
   * ends with it, or declares those group names itself.
   */
  public static final String LITERAL_OR_CONSTANT =
      "(?:\"(?<literal>\\w+)\"|(?<constant>(?:\\w+\\.)*[A-Z][A-Z0-9_]*))(?=\\s*[,)])";

  /** The simple-name prefixes of the msgId-carrying exceptions ({@code <prefix>Exception}). */
  public static final List<String> DEFAULT_EXCEPTION_PREFIXES =
      List.of(
          "ItemNotFound",
          "BadRequest",
          "ItemAlreadyExist",
          "AccessNotAllowed",
          "UnAuthorized",
          "ErrorOccurred",
          "SignatureNotVerified",
          "Mfa",
          "DataExportRequest",
          "SubscriptionLimitReached");

  /** The variable names a response factory is called through. */
  public static final List<String> DEFAULT_RESPONSE_RECEIVERS =
      List.of("resp", "responseFactory", "restResponseFactory");

  private static final Pattern CONSTANT_DEFINITION =
      Pattern.compile(
          "static\\s+final\\s+(?:@\\w+\\s+)?String\\s+([A-Z][A-Z0-9_]*)\\s*=\\s*\"([^\"\\\\]*)\"");

  private final List<Pattern> msgIdPatterns;
  private final Pattern nonIdentifierPattern;

  private MsgIdSourceScanner(final Builder builder) {
    final var exceptionStart =
        "\\bnew\\s+(?:"
            + String.join("|", builder.exceptionPrefixes.stream().map(Pattern::quote).toList())
            + ")Exception\\(\\s*";
    final var responseStart =
        "\\b(?:"
            + String.join("|", builder.responseReceivers.stream().map(Pattern::quote).toList())
            + ")\\s*\\.\\s*(?:success|warning|error)\\(\\s*";

    final var patterns = new ArrayList<Pattern>();

    patterns.add(Pattern.compile(responseStart + LITERAL_OR_CONSTANT));
    patterns.add(Pattern.compile(exceptionStart + LITERAL_OR_CONSTANT));
    patterns.addAll(builder.extraPatterns);

    this.msgIdPatterns = List.copyOf(patterns);
    this.nonIdentifierPattern =
        Pattern.compile(
            exceptionStart
                + "(?:\"(?<text>(?:[^\"\\\\]|\\\\.)*)\"\\s*(?<next>[,)+]|\\.\\s*formatted\\s*\\()"
                + "|(?<call>(?:String|MessageFormat)\\s*\\.\\s*format\\s*\\())");
  }

  /**
   * Starts a scanner with the default exceptions and response receivers.
   *
   * @return a builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The msgIds of one set of sources.
   *
   * @param usedMsgIds msgId to the files (relative paths) that use it
   * @param unresolvedConstants constants that no scanned class defines, with the file that uses
   *     them
   * @param nonIdentifierMsgIds free-text or built exception msgIds to the files that throw them
   */
  public record Scan(
      Map<String, Set<String>> usedMsgIds,
      Set<String> unresolvedConstants,
      Map<String, Set<String>> nonIdentifierMsgIds) {}

  /**
   * The msgIds in {@code sources}.
   *
   * @param sources file name to source
   * @param constants constant name to its values, from {@link #collectConstants(Iterable)} of every
   *     source that may define a constant {@code sources} uses
   * @return what the scan found
   */
  public Scan scan(final Map<String, String> sources, final Map<String, Set<String>> constants) {
    final var used = new TreeMap<String, Set<String>>();
    final var unresolved = new TreeSet<String>();
    final var nonIdentifiers = new TreeMap<String, Set<String>>();

    sources.forEach(
        (file, source) -> {
          for (final var pattern : this.msgIdPatterns) {
            collect(pattern, source, file, constants, used, unresolved);
          }

          this.findNonIdentifierMsgIds(source)
              .forEach(
                  text -> nonIdentifiers.computeIfAbsent(text, key -> new TreeSet<>()).add(file));
        });

    return new Scan(used, unresolved, nonIdentifiers);
  }

  /**
   * The msgId arguments in {@code source} that are free text, or continued by a concatenation,
   * {@code .formatted(} or {@code String.format(}, as the text of each.
   *
   * @param source Java source
   * @return the offending texts in source order ({@code String.format(} for a call)
   */
  public List<String> findNonIdentifierMsgIds(final String source) {
    final var found = new ArrayList<String>();
    final Matcher matcher = this.nonIdentifierPattern.matcher(source);

    while (matcher.find()) {
      final var call = matcher.group("call");

      if (call != null) {
        found.add(call.replaceAll("\\s", ""));
        continue;
      }

      final var text = matcher.group("text");
      final var next = matcher.group("next");
      final var continued = !",".equals(next) && !")".equals(next);

      if (continued || !text.matches("\\w+")) {
        found.add(text);
      }
    }

    return found;
  }

  /**
   * Every {@code static final String NAME = "value"} in the sources, by name. A name may be reused
   * across classes, so each name maps to all its values.
   *
   * @param sourceSets the sources of each set
   * @return constant name to values
   */
  @SafeVarargs
  public static Map<String, Set<String>> collectConstants(final Iterable<String>... sourceSets) {
    final var constants = new HashMap<String, Set<String>>();

    for (final var sources : sourceSets) {
      for (final var source : sources) {
        final var matcher = CONSTANT_DEFINITION.matcher(source);

        while (matcher.find()) {
          constants.computeIfAbsent(matcher.group(1), key -> new HashSet<>()).add(matcher.group(2));
        }
      }
    }

    return constants;
  }

  /**
   * The {@code src/main/java} directories at most {@code depth} levels below {@code dir}.
   *
   * @param dir the directory to search
   * @param depth the maximum depth
   * @return the source roots
   * @throws IOException when {@code dir} cannot be walked
   */
  public static List<Path> findSourceRoots(final Path dir, final int depth) throws IOException {
    try (Stream<Path> walk = Files.walk(dir, depth)) {
      return walk.filter(path -> path.endsWith(Path.of("src", "main", "java"))).toList();
    }
  }

  /**
   * Reads every {@code .java} file under the roots.
   *
   * @param roots the source roots
   * @return path relative to its root to the source, sorted
   * @throws IOException when a root cannot be walked
   */
  public static Map<String, String> readSources(final Iterable<Path> roots) throws IOException {
    final var sources = new TreeMap<String, String>();

    for (final var root : roots) {
      try (Stream<Path> walk = Files.walk(root)) {
        walk.filter(path -> path.toString().endsWith(".java"))
            .forEach(path -> sources.put(root.relativize(path).toString(), read(path)));
      }
    }

    return sources;
  }

  private static void collect(
      final Pattern pattern,
      final String source,
      final String file,
      final Map<String, Set<String>> constants,
      final Map<String, Set<String>> used,
      final Set<String> unresolved) {
    final Matcher matcher = pattern.matcher(source);

    while (matcher.find()) {
      final var literal = groupOrNull(matcher, "literal");
      final var constant = groupOrNull(matcher, "constant");

      if (literal != null) {
        used.computeIfAbsent(literal, key -> new TreeSet<>()).add(file);
      } else if (constant != null) {
        final var values = constants.get(constant.substring(constant.lastIndexOf('.') + 1));

        if (values == null) {
          unresolved.add(constant + " (" + file + ")");
        } else {
          values.forEach(value -> used.computeIfAbsent(value, key -> new TreeSet<>()).add(file));
        }
      }
    }
  }

  /** {@code Matcher.group(name)} throws when the pattern has no group of that name. */
  private static @Nullable String groupOrNull(final Matcher matcher, final String name) {
    return matcher.pattern().namedGroups().containsKey(name) ? matcher.group(name) : null;
  }

  private static String read(final Path path) {
    try {
      return Files.readString(path);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Configures a {@link MsgIdSourceScanner}. */
  public static final class Builder {

    private List<String> exceptionPrefixes = DEFAULT_EXCEPTION_PREFIXES;
    private List<String> responseReceivers = DEFAULT_RESPONSE_RECEIVERS;
    private final List<Pattern> extraPatterns = new ArrayList<>();

    private Builder() {}

    /**
     * Replaces the msgId-carrying exceptions ({@link #DEFAULT_EXCEPTION_PREFIXES}).
     *
     * @param prefixes simple-name prefixes, each followed by {@code Exception}
     * @return this builder
     */
    public Builder exceptionPrefixes(final List<String> prefixes) {
      this.exceptionPrefixes = List.copyOf(prefixes);
      return this;
    }

    /**
     * Replaces the response factory variable names ({@link #DEFAULT_RESPONSE_RECEIVERS}).
     *
     * @param receivers variable names
     * @return this builder
     */
    public Builder responseReceivers(final List<String> receivers) {
      this.responseReceivers = List.copyOf(receivers);
      return this;
    }

    /**
     * Adds a product-specific call shape. The pattern declares the named group {@code literal},
     * {@code constant} or both, usually by ending with {@link #LITERAL_OR_CONSTANT}.
     *
     * @param pattern the pattern
     * @return this builder
     */
    public Builder extraPattern(final Pattern pattern) {
      this.extraPatterns.add(pattern);
      return this;
    }

    /**
     * Builds the scanner.
     *
     * @return the scanner
     */
    public MsgIdSourceScanner build() {
      return new MsgIdSourceScanner(this);
    }
  }
}
