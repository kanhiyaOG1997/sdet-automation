package io.github.kanhiyaog1997.sdet.core.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable, thread-safe view of the test configuration.
 *
 * <p>Each key is resolved in this order, highest precedence first:
 *
 * <ol>
 *   <li>JVM system property, for example {@code -Dapi.base-url=...}
 *   <li>environment variable, for example {@code API_BASE_URL}
 *   <li>classpath resource {@code config/<environment>.yml}
 *   <li>classpath resource {@code config/default.yml}
 * </ol>
 *
 * <p>Keys in the {@code secret.} namespace are different: they can only come from environment
 * variables (for example {@code secret.admin-password} from {@code SECRET_ADMIN_PASSWORD}) and can
 * only be read through {@link #requireSecret(String)}. They are rejected in YAML files and as
 * system properties, because system properties are visible in process listings, end up in shell
 * history and are copied into Surefire XML reports.
 */
public final class TestConfig {

  static final String SECRET_PREFIX = "secret.";
  private static final String SECRET_ENV_PREFIX = "SECRET_";
  private static final Pattern SIMPLE_DURATION = Pattern.compile("(\\d{1,15})(ms|s|m|h)");

  private static volatile TestConfig current;

  private final String environment;
  private final Map<String, String> fileValues;
  private final Map<String, String> environmentVariables;
  private final Map<String, String> systemProperties;
  private final List<String> fileSources;

  TestConfig(
      String environment,
      Map<String, String> fileValues,
      Map<String, String> environmentVariables,
      Map<String, String> systemProperties,
      List<String> fileSources) {
    this.environment = environment;
    this.fileValues = Map.copyOf(fileValues);
    this.environmentVariables = Map.copyOf(environmentVariables);
    this.systemProperties = Map.copyOf(systemProperties);
    this.fileSources = List.copyOf(fileSources);
  }

  /**
   * Loads configuration from the real process environment, system properties and classpath.
   *
   * @return a new configuration
   * @throws ConfigException if the configuration is invalid
   */
  public static TestConfig load() {
    return ConfigLoader.fromRuntime().load();
  }

  /**
   * Returns a configuration loaded once per JVM and cached.
   *
   * <p>Intended only for code that cannot receive a configuration object, such as TestNG listeners,
   * which TestNG creates through no-argument constructors. Everything else should receive a {@code
   * TestConfig} explicitly. If loading fails, the next call tries again.
   *
   * @return the shared configuration
   * @throws ConfigException if the configuration is invalid
   */
  public static TestConfig current() {
    TestConfig result = current;
    if (result == null) {
      synchronized (TestConfig.class) {
        result = current;
        if (result == null) {
          result = load();
          current = result;
        }
      }
    }
    return result;
  }

  /**
   * Returns the selected environment name, for example {@code local} or {@code staging}.
   *
   * @return the environment name
   */
  public String environment() {
    return environment;
  }

  /**
   * Looks up an optional, non-secret value.
   *
   * @param key a dotted key such as {@code api.base-url}
   * @return the value, or empty if no source defines the key
   * @throws IllegalArgumentException if {@code key} is a secret key
   */
  public Optional<String> find(String key) {
    rejectSecretKey(key);
    return lookup(key);
  }

  /**
   * Returns a required, non-secret value.
   *
   * @param key a dotted key such as {@code api.base-url}
   * @return the value
   * @throws ConfigException if no source defines the key
   * @throws IllegalArgumentException if {@code key} is a secret key
   */
  public String require(String key) {
    return find(key).orElseThrow(() -> new ConfigException(missingMessage(key)));
  }

  /**
   * Returns a required integer value.
   *
   * @param key the key
   * @return the parsed integer
   * @throws ConfigException if the key is missing or not an integer
   */
  public int requireInt(String key) {
    String value = require(key).trim();
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new ConfigException(invalidMessage(key, value, "an integer"), e);
    }
  }

  /**
   * Returns a required boolean value. Only {@code true} and {@code false} are accepted, in any
   * letter case.
   *
   * @param key the key
   * @return the parsed boolean
   * @throws ConfigException if the key is missing or not {@code true} or {@code false}
   */
  public boolean requireBoolean(String key) {
    String value = require(key).trim();
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new ConfigException(invalidMessage(key, value, "true or false"));
  }

  /**
   * Returns a required duration, written either as a number with a unit ({@code 500ms}, {@code
   * 30s}, {@code 5m}, {@code 2h}) or in ISO-8601 form ({@code PT30S}).
   *
   * @param key the key
   * @return the parsed duration
   * @throws ConfigException if the key is missing or not a valid duration
   */
  public Duration requireDuration(String key) {
    String value = require(key).trim();
    Matcher simple = SIMPLE_DURATION.matcher(value);
    if (simple.matches()) {
      long amount = Long.parseLong(simple.group(1));
      return switch (simple.group(2)) {
        case "ms" -> Duration.ofMillis(amount);
        case "s" -> Duration.ofSeconds(amount);
        case "m" -> Duration.ofMinutes(amount);
        default -> Duration.ofHours(amount);
      };
    }
    try {
      return Duration.parse(value);
    } catch (DateTimeParseException e) {
      throw new ConfigException(
          invalidMessage(key, value, "a duration such as 500ms, 30s, 5m, 2h or PT30S"), e);
    }
  }

  /**
   * Returns a required absolute URI, such as a base URL.
   *
   * @param key the key
   * @return the parsed URI
   * @throws ConfigException if the key is missing, not a valid URI, or not absolute
   */
  public URI requireUri(String key) {
    String value = require(key).trim();
    URI uri;
    try {
      uri = new URI(value);
    } catch (URISyntaxException e) {
      throw new ConfigException(invalidMessage(key, value, "a valid URI"), e);
    }
    if (!uri.isAbsolute()) {
      throw new ConfigException(invalidMessage(key, value, "an absolute URI with a scheme"));
    }
    return uri;
  }

  /**
   * Returns a required secret from its environment variable.
   *
   * @param key a key in the {@code secret.} namespace, such as {@code secret.admin-password}, read
   *     from {@code SECRET_ADMIN_PASSWORD}
   * @return the secret, masked when printed
   * @throws ConfigException if the environment variable is not set
   * @throws IllegalArgumentException if {@code key} is not in the {@code secret.} namespace
   */
  public Secret requireSecret(String key) {
    Objects.requireNonNull(key, "key");
    if (!key.startsWith(SECRET_PREFIX)) {
      throw new IllegalArgumentException(
          "Secret keys must start with '" + SECRET_PREFIX + "', but got '" + key + "'");
    }
    String value = environmentVariables.get(environmentVariableName(key));
    if (value == null) {
      throw new ConfigException(
          "Missing required secret '"
              + key
              + "'. Supply it via environment variable "
              + environmentVariableName(key)
              + ".");
    }
    return new Secret(value);
  }

  /**
   * Returns every key defined in the YAML files with its effective value, sorted by key. Suitable
   * for report environment information: it can never contain secrets, because secret keys are
   * rejected in files.
   *
   * @return an unmodifiable, sorted map
   */
  public Map<String, String> nonSecretEntries() {
    Map<String, String> entries = new TreeMap<>();
    for (String key : fileValues.keySet()) {
      entries.put(key, lookup(key).orElseThrow());
    }
    return Collections.unmodifiableMap(entries);
  }

  /**
   * Returns the values of every secret supplied to this run, so they can be masked wherever they
   * might appear in logs or reports. These are the non-empty environment variables named {@code
   * SECRET_*}, the only supported source of secrets.
   *
   * @return an unmodifiable set of non-empty secret values
   */
  public Set<String> secretValues() {
    Set<String> values = new HashSet<>();
    environmentVariables.forEach(
        (name, value) -> {
          if (name.startsWith(SECRET_ENV_PREFIX) && !value.isEmpty()) {
            values.add(value);
          }
        });
    return Set.copyOf(values);
  }

  /**
   * Maps a dotted key to its environment variable name: upper case, with dots and dashes replaced
   * by underscores. For example {@code api.base-url} becomes {@code API_BASE_URL}.
   *
   * @param key the dotted key
   * @return the environment variable name
   */
  static String environmentVariableName(String key) {
    return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
  }

  private Optional<String> lookup(String key) {
    String value = systemProperties.get(key);
    if (value == null) {
      value = environmentVariables.get(environmentVariableName(key));
    }
    if (value == null) {
      value = fileValues.get(key);
    }
    return Optional.ofNullable(value);
  }

  private static void rejectSecretKey(String key) {
    Objects.requireNonNull(key, "key");
    if (key.startsWith(SECRET_PREFIX)) {
      throw new IllegalArgumentException("Use requireSecret(\"" + key + "\") to read secrets");
    }
  }

  private String missingMessage(String key) {
    return "Missing required configuration key '"
        + key
        + "' for environment '"
        + environment
        + "'. Looked in: system property '"
        + key
        + "', environment variable '"
        + environmentVariableName(key)
        + "', "
        + String.join(", ", fileSources.stream().map(s -> "classpath:" + s).toList())
        + ".";
  }

  private static String invalidMessage(String key, String value, String expected) {
    return "Configuration key '" + key + "' must be " + expected + ", but was '" + value + "'.";
  }
}
