package io.github.kanhiyaog1997.sdet.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.core.JsonLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Behaviour of layered configuration loading, typed access and secret handling. */
public class TestConfigTest {

  private Path classpathRoot;
  private final List<URLClassLoader> loaders = new ArrayList<>();

  /** Creates an isolated classpath root so each test controls exactly which YAML files exist. */
  @BeforeMethod
  public void createClasspathRoot() throws IOException {
    classpathRoot = Files.createTempDirectory("test-config");
    Files.createDirectories(classpathRoot.resolve("config"));
  }

  /** Closes the class loaders and deletes the temporary classpath root. */
  @AfterMethod(alwaysRun = true)
  public void deleteClasspathRoot() throws IOException {
    for (URLClassLoader loader : loaders) {
      loader.close();
    }
    loaders.clear();
    try (Stream<Path> paths = Files.walk(classpathRoot)) {
      paths.sorted(Comparator.reverseOrder()).forEach(TestConfigTest::delete);
    }
  }

  // ---- precedence and key mapping ----

  @Test
  public void laterLayersOverrideEarlierOnes() {
    yaml("default.yml", "a: default\nb: default\nc: default\nd: default\n");
    yaml("local.yml", "b: env-file\nc: env-file\nd: env-file\n");

    TestConfig config =
        load(Map.of("C", "env-var", "D", "env-var"), Map.of("d", "system-property"));

    assertThat(config.require("a")).isEqualTo("default");
    assertThat(config.require("b")).isEqualTo("env-file");
    assertThat(config.require("c")).isEqualTo("env-var");
    assertThat(config.require("d")).isEqualTo("system-property");
  }

  @Test
  public void nestedYamlKeysAreFlattenedAndMappedToEnvironmentVariableNames() {
    yaml("default.yml", "api:\n  base-url: http://from-file\n  timeout: 5s\n");

    TestConfig config = load(Map.of("API_BASE_URL", "http://from-env"), Map.of());

    assertThat(config.require("api.base-url")).isEqualTo("http://from-env");
    assertThat(config.require("api.timeout")).isEqualTo("5s");
    assertThat(TestConfig.environmentVariableName("api.base-url")).isEqualTo("API_BASE_URL");
  }

  @Test
  public void keysCanComeFromEnvironmentVariablesAndSystemPropertiesAlone() {
    TestConfig config = load(Map.of("ONLY_ENV", "e"), Map.of("only.property", "p"));

    assertThat(config.require("only.env")).isEqualTo("e");
    assertThat(config.require("only.property")).isEqualTo("p");
  }

  @Test
  public void findReturnsEmptyForAnUndefinedKey() {
    assertThat(load(Map.of(), Map.of()).find("not.defined")).isEmpty();
  }

  @Test
  public void missingKeyMessageNamesEverySourceConsulted() {
    TestConfig config = load(Map.of(), Map.of());

    assertThatThrownBy(() -> config.require("api.base-url"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("'api.base-url'")
        .hasMessageContaining("environment 'local'")
        .hasMessageContaining("system property 'api.base-url'")
        .hasMessageContaining("environment variable 'API_BASE_URL'")
        .hasMessageContaining("classpath:config/local.yml")
        .hasMessageContaining("classpath:config/default.yml");
  }

  // ---- environment selection ----

  @Test
  public void defaultEnvironmentIsLocalAndItsFileIsOptional() {
    TestConfig config = load(Map.of(), Map.of());

    assertThat(config.environment()).isEqualTo("local");
  }

  @Test
  public void environmentIsSelectedBySystemPropertyBeforeEnvironmentVariable() {
    yaml("staging.yml", "name: staging\n");
    yaml("qa.yml", "name: qa\n");

    TestConfig config = load(Map.of("TEST_ENV", "qa"), Map.of("test.env", "staging"));

    assertThat(config.environment()).isEqualTo("staging");
    assertThat(config.require("name")).isEqualTo("staging");
  }

  @Test
  public void environmentVariableSelectsEnvironmentWhenNoSystemPropertyIsSet() {
    yaml("qa.yml", "name: qa\n");

    TestConfig config = load(Map.of("TEST_ENV", " qa "), Map.of("test.env", " "));

    assertThat(config.environment()).isEqualTo("qa");
  }

  @Test
  public void explicitlySelectedEnvironmentWithoutFileFailsFast() {
    assertThatThrownBy(() -> load(Map.of("TEST_ENV", "staging"), Map.of()))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("Environment 'staging' was selected")
        .hasMessageContaining("config/staging.yml does not exist");
  }

  @Test
  public void environmentNameCannotEscapeTheConfigDirectory() {
    assertThatThrownBy(() -> load(Map.of(), Map.of("test.env", "../secrets")))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("Invalid environment name '../secrets'");
  }

  // ---- typed access ----

  @Test
  public void typedAccessorsParseValidValues() {
    yaml(
        "default.yml",
        "port: 8080\nenabled: TRUE\ndisabled: false\nbase: https://example.test/api\n"
            + "ms: 500ms\nsec: 30s\nmin: 5m\nhours: 2h\niso: PT1M30S\n");

    TestConfig config = load(Map.of(), Map.of());

    assertThat(config.requireInt("port")).isEqualTo(8080);
    assertThat(config.requireBoolean("enabled")).isTrue();
    assertThat(config.requireBoolean("disabled")).isFalse();
    assertThat(config.requireUri("base")).isEqualTo(URI.create("https://example.test/api"));
    assertThat(config.requireDuration("ms")).isEqualTo(Duration.ofMillis(500));
    assertThat(config.requireDuration("sec")).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.requireDuration("min")).isEqualTo(Duration.ofMinutes(5));
    assertThat(config.requireDuration("hours")).isEqualTo(Duration.ofHours(2));
    assertThat(config.requireDuration("iso")).isEqualTo(Duration.ofSeconds(90));
  }

  @Test
  public void typedAccessorsRejectInvalidValuesWithTheKeyAndValueInTheMessage() {
    yaml(
        "default.yml",
        "port: eighty\nflag: yes-please\nwait: soon\nrelative: /api\nbroken: 'http://a b'\n");

    TestConfig config = load(Map.of(), Map.of());

    assertThatThrownBy(() -> config.requireInt("port"))
        .isInstanceOf(ConfigException.class)
        .hasMessage("Configuration key 'port' must be an integer, but was 'eighty'.");
    assertThatThrownBy(() -> config.requireBoolean("flag"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("must be true or false");
    assertThatThrownBy(() -> config.requireDuration("wait"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("must be a duration");
    assertThatThrownBy(() -> config.requireUri("relative"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("must be an absolute URI");
    assertThatThrownBy(() -> config.requireUri("broken"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("must be a valid URI");
  }

  // ---- secrets ----

  @Test
  public void secretsInYamlAreRejectedWithInstructionsForTheRightSource() {
    yaml("default.yml", "secret:\n  admin-password: hunter2\n");

    assertThatThrownBy(() -> load(Map.of(), Map.of()))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("'secret.admin-password' in classpath:config/default.yml")
        .hasMessageContaining("environment variable SECRET_ADMIN_PASSWORD")
        .hasMessageContaining("-Dsecret.admin-password")
        .hasMessageNotContaining("hunter2");
  }

  @Test
  public void secretsInTheEnvironmentFileAreRejectedToo() {
    yaml("qa.yml", "secret.token: abc\n");

    assertThatThrownBy(() -> load(Map.of(), Map.of("test.env", "qa")))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("classpath:config/qa.yml");
  }

  @Test
  public void secretsComeFromSystemPropertiesBeforeEnvironmentVariables() {
    TestConfig fromEnv = load(Map.of("SECRET_TOKEN", "env-token"), Map.of());
    TestConfig fromBoth =
        load(Map.of("SECRET_TOKEN", "env-token"), Map.of("secret.token", "prop-token"));

    assertThat(fromEnv.requireSecret("secret.token").reveal()).isEqualTo("env-token");
    assertThat(fromBoth.requireSecret("secret.token").reveal()).isEqualTo("prop-token");
  }

  @Test
  public void missingSecretMessageNamesBothSourcesButNoValue() {
    assertThatThrownBy(() -> load(Map.of(), Map.of()).requireSecret("secret.api-key"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("environment variable SECRET_API_KEY")
        .hasMessageContaining("system property -Dsecret.api-key");
  }

  @Test
  public void secretsAndPlainValuesUseSeparateAccessors() {
    TestConfig config = load(Map.of("SECRET_TOKEN", "t"), Map.of());

    assertThatThrownBy(() -> config.require("secret.token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requireSecret");
    assertThatThrownBy(() -> config.requireSecret("api.token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must start with 'secret.'");
  }

  @Test
  public void secretIsMaskedWhenPrinted() {
    Secret secret = load(Map.of("SECRET_TOKEN", "s3cr3t"), Map.of()).requireSecret("secret.token");

    assertThat(secret).hasToString("****");
    assertThat("token=" + secret).doesNotContain("s3cr3t");
  }

  @Test
  public void secretValuesCoverSecretSystemPropertiesAndEnvironmentVariablesOnly() {
    TestConfig config =
        load(
            Map.of("SECRET_A", "from-env", "SECRET_EMPTY", "", "PATH", "/usr/bin"),
            Map.of("secret.b", "from-prop", "user.name", "someone"));

    assertThat(config.secretValues()).containsExactlyInAnyOrder("from-env", "from-prop");
  }

  // ---- report information and immutability ----

  @Test
  public void nonSecretEntriesAreSortedEffectiveValuesWithoutSecrets() {
    yaml("default.yml", "zeta: 1\nalpha: file\n");

    TestConfig config =
        load(Map.of("ALPHA", "override", "SECRET_TOKEN", "t"), Map.of("secret.other", "o"));

    assertThat(config.nonSecretEntries())
        .containsExactly(Map.entry("alpha", "override"), Map.entry("zeta", "1"));
  }

  @Test
  public void configurationCannotBeChangedAfterLoading() {
    Map<String, String> environment = new HashMap<>(Map.of("NAME", "before"));
    yaml("default.yml", "key: value\n");
    TestConfig config = load(environment, Map.of());

    environment.put("NAME", "after");

    assertThat(config.require("name")).isEqualTo("before");
    assertThatThrownBy(() -> config.nonSecretEntries().put("key", "changed"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> config.secretValues().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // ---- YAML validation ----

  @Test
  public void emptyYamlFileContributesNothing() {
    yaml("default.yml", "");

    assertThat(load(Map.of(), Map.of()).nonSecretEntries()).isEmpty();
  }

  @Test
  public void nullAndBlankYamlValuesAreUndefinedButQuotedEmptyStringsAreKept() {
    yaml("default.yml", "defined: present\nblank:\nexplicit: null\ntilde: ~\nquoted: ''\n");

    TestConfig config = load(Map.of(), Map.of());

    assertThat(config.find("defined")).contains("present");
    assertThat(config.find("blank")).isEmpty();
    assertThat(config.find("explicit")).isEmpty();
    assertThat(config.find("tilde")).isEmpty();
    assertThat(config.find("quoted")).as("an explicitly quoted empty string is kept").contains("");
  }

  @Test
  public void invalidYamlStructuresAreRejected() {
    assertLoadFails("browsers:\n  - chrome\n  - firefox\n", "Lists are not supported");
    assertLoadFails("just a scalar\n", "must contain key-value pairs at the top level");
    assertLoadFails("a: 1\na: 2\n", "is not valid YAML");
    assertLoadFails("a.b: 1\na:\n  b: 2\n", "Key 'a.b' is defined twice");
    assertLoadFails("a: [unclosed\n", "is not valid YAML");
  }

  // ---- S1: malformed YAML must never echo file content ----

  @Test
  public void malformedYamlErrorsNeverEchoFileContentButKeepThePosition() {
    String secretValue = "LEAKED-VALUE-42";

    assertMalformedYamlHidesSecret(
        "name: ok\nsecret.token: " + secretValue + ": bad\nother: value\n",
        secretValue,
        "line 2, column 30");
    assertMalformedYamlHidesSecret("secret.token: [" + secretValue + "\n", secretValue, "line 1");
    assertMalformedYamlHidesSecret(
        "name: ok\nsecret.token: \"" + secretValue + "\n", secretValue, "line 2");
  }

  @Test
  public void invalidYamlMessageOmitsThePositionWhenTheParserHasNone() {
    String expected =
        "classpath:config/x.yml is not valid YAML. Parser details are omitted because they can"
            + " quote file content, including secrets.";

    assertThat(ConfigLoader.invalidYamlMessage("config/x.yml", null)).isEqualTo(expected);
    assertThat(ConfigLoader.invalidYamlMessage("config/x.yml", JsonLocation.NA))
        .isEqualTo(expected);
  }

  // ---- shared instance ----

  @Test
  public void currentReturnsOneSharedInstanceAcrossThreads() throws Exception {
    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Callable<TestConfig>> tasks = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        tasks.add(
            () -> {
              start.await();
              return TestConfig.current();
            });
      }
      List<Future<TestConfig>> futures = new ArrayList<>();
      for (Callable<TestConfig> task : tasks) {
        futures.add(pool.submit(task));
      }
      start.countDown();
      TestConfig first = futures.get(0).get();
      for (Future<TestConfig> future : futures) {
        assertThat(future.get()).isSameAs(first);
      }
      assertThat(TestConfig.current()).isSameAs(first);
    } finally {
      pool.shutdownNow();
    }
  }

  // ---- helpers ----

  private void assertLoadFails(String yaml, String expectedMessage) {
    yaml("default.yml", yaml);
    assertThatThrownBy(() -> load(Map.of(), Map.of()))
        .as("YAML: %s", yaml)
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(expectedMessage);
  }

  private void assertMalformedYamlHidesSecret(String yaml, String secretValue, String position) {
    yaml("default.yml", yaml);

    Throwable thrown = catchThrowable(() -> load(Map.of(), Map.of()));

    assertThat(thrown)
        .as("YAML: %s", yaml)
        .isInstanceOf(ConfigException.class)
        .hasMessageStartingWith("classpath:config/default.yml is not valid YAML (" + position)
        .hasNoCause();
    for (Throwable t = thrown; t != null; t = t.getCause()) {
      assertThat(t.getMessage()).doesNotContain(secretValue);
      assertThat(t.toString()).doesNotContain(secretValue);
      assertThat(t.getSuppressed()).isEmpty();
    }
    StringWriter stackTrace = new StringWriter();
    thrown.printStackTrace(new PrintWriter(stackTrace));
    assertThat(stackTrace.toString()).doesNotContain(secretValue);
  }

  private TestConfig load(Map<String, String> environment, Map<String, String> properties) {
    try {
      URLClassLoader loader = new URLClassLoader(new URL[] {classpathRoot.toUri().toURL()}, null);
      loaders.add(loader);
      return new ConfigLoader(environment, properties, loader).load();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void yaml(String fileName, String content) {
    try {
      Files.writeString(
          classpathRoot.resolve("config").resolve(fileName), content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void delete(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
