package io.github.kanhiyaog1997.sdet.core.config;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a {@link TestConfig} from YAML files on the classpath plus the environment variables and
 * system properties it is given.
 *
 * <p>The environment variables and system properties are passed in as maps rather than read
 * directly, so tests can supply exact inputs without touching the real process environment.
 */
final class ConfigLoader {

  static final String ENVIRONMENT_PROPERTY = "test.env";
  static final String ENVIRONMENT_VARIABLE = "TEST_ENV";
  static final String DEFAULT_ENVIRONMENT = "local";
  static final String DEFAULT_RESOURCE = "config/default.yml";

  private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);
  private static final Pattern ENVIRONMENT_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");
  private static final ObjectMapper YAML =
      new ObjectMapper(
          YAMLFactory.builder()
              .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
              // YAML defines `key:` with no value as null; without this Jackson reads "".
              .enable(YAMLParser.Feature.EMPTY_STRING_AS_NULL)
              .build());

  private final Map<String, String> environmentVariables;
  private final Map<String, String> systemProperties;
  private final ClassLoader classLoader;

  ConfigLoader(
      Map<String, String> environmentVariables,
      Map<String, String> systemProperties,
      ClassLoader classLoader) {
    this.environmentVariables = Map.copyOf(environmentVariables);
    this.systemProperties = Map.copyOf(systemProperties);
    this.classLoader = classLoader;
  }

  static ConfigLoader fromRuntime() {
    Map<String, String> properties = new HashMap<>();
    System.getProperties()
        .stringPropertyNames()
        .forEach(name -> properties.put(name, System.getProperty(name)));
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = ConfigLoader.class.getClassLoader();
    }
    return new ConfigLoader(System.getenv(), properties, loader);
  }

  TestConfig load() {
    rejectSecretSystemProperties();
    Optional<String> selected = selectedEnvironment();
    String environment = selected.orElse(DEFAULT_ENVIRONMENT);
    if (!ENVIRONMENT_NAME.matcher(environment).matches()) {
      throw new ConfigException(
          "Invalid environment name '"
              + environment
              + "': use lower-case letters, digits and dashes, for example 'staging'.");
    }
    String environmentResource = "config/" + environment + ".yml";

    Map<String, String> values = new HashMap<>(readYaml(DEFAULT_RESOURCE).orElse(Map.of()));
    Optional<Map<String, String>> environmentValues = readYaml(environmentResource);
    if (environmentValues.isEmpty() && selected.isPresent()) {
      throw new ConfigException(
          "Environment '"
              + environment
              + "' was selected via -D"
              + ENVIRONMENT_PROPERTY
              + " or "
              + ENVIRONMENT_VARIABLE
              + ", but classpath resource "
              + environmentResource
              + " does not exist.");
    }
    environmentValues.ifPresent(values::putAll);

    LOG.info(
        "Loaded test configuration for environment '{}': {} keys from {} and {}",
        environment,
        values.size(),
        DEFAULT_RESOURCE,
        environmentResource);
    return new TestConfig(
        environment,
        values,
        environmentVariables,
        systemProperties,
        List.of(environmentResource, DEFAULT_RESOURCE));
  }

  /**
   * Fails if any secret is supplied as a system property. The message names each property and the
   * environment variable to use instead, never the value.
   */
  private void rejectSecretSystemProperties() {
    List<String> rejected =
        systemProperties.keySet().stream()
            .filter(name -> name.startsWith(TestConfig.SECRET_PREFIX))
            .sorted()
            .map(name -> "-D" + name + " (use " + TestConfig.environmentVariableName(name) + ")")
            .toList();
    if (!rejected.isEmpty()) {
      throw new ConfigException(
          "Secrets must not be supplied as system properties, because they are visible in process"
              + " listings and are copied into test reports. Supply them as environment variables"
              + " instead: "
              + String.join(", ", rejected)
              + ".");
    }
  }

  private Optional<String> selectedEnvironment() {
    String value = systemProperties.get(ENVIRONMENT_PROPERTY);
    if (value == null || value.isBlank()) {
      value = environmentVariables.get(ENVIRONMENT_VARIABLE);
    }
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }

  private Optional<Map<String, String>> readYaml(String resource) {
    try (InputStream in = classLoader.getResourceAsStream(resource)) {
      if (in == null) {
        return Optional.empty();
      }
      JsonNode root = YAML.readTree(in);
      Map<String, String> values = new HashMap<>();
      if (root == null || root.isMissingNode() || root.isNull()) {
        return Optional.of(values);
      }
      if (!root.isObject()) {
        throw new ConfigException(
            "classpath:" + resource + " must contain key-value pairs at the top level.");
      }
      flatten("", root, resource, values);
      return Optional.of(values);
    } catch (JsonProcessingException e) {
      // Deliberately neither e.getMessage() nor e as the cause: YAML parser messages quote the
      // offending source line, which can contain a secret. Only the position is reported.
      throw new ConfigException(invalidYamlMessage(resource, e.getLocation()));
    } catch (IOException e) {
      throw new ConfigException("Could not read classpath:" + resource, e);
    }
  }

  static String invalidYamlMessage(String resource, JsonLocation location) {
    String position =
        location == null || location.getLineNr() < 1
            ? ""
            : " (line " + location.getLineNr() + ", column " + location.getColumnNr() + ")";
    return "classpath:"
        + resource
        + " is not valid YAML"
        + position
        + ". Parser details are omitted because they can quote file content, including secrets.";
  }

  private static void flatten(
      String prefix, JsonNode node, String resource, Map<String, String> out) {
    for (Map.Entry<String, JsonNode> field : node.properties()) {
      String key = prefix + field.getKey();
      JsonNode value = field.getValue();
      if (value.isObject()) {
        flatten(key + ".", value, resource, out);
      } else if (value.isArray()) {
        throw new ConfigException(
            "Lists are not supported in test configuration (key '"
                + key
                + "' in classpath:"
                + resource
                + ").");
      } else if (!value.isNull()) {
        if (key.startsWith(TestConfig.SECRET_PREFIX)) {
          throw new ConfigException(
              "'"
                  + key
                  + "' in classpath:"
                  + resource
                  + " is a secret and must not be stored in a file. Supply it via environment"
                  + " variable "
                  + TestConfig.environmentVariableName(key)
                  + ".");
        }
        if (out.putIfAbsent(key, value.asText()) != null) {
          throw new ConfigException(
              "Key '" + key + "' is defined twice in classpath:" + resource + ".");
        }
      }
    }
  }
}
