package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.kanhiyaog1997.sdet.core.config.ConfigException;
import io.github.kanhiyaog1997.sdet.core.config.TestConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** The environment file holds only safe information, and handles failures without leaking. */
public class AllureEnvironmentListenerTest {

  private static final String SECRET = "TASK5-SECRET-12345";

  private Path directory;

  /** Creates an empty results directory for each test. */
  @BeforeMethod
  public void createDirectory() throws IOException {
    directory = Files.createTempDirectory("allure-environment");
  }

  /** Deletes the results directory. */
  @AfterMethod(alwaysRun = true)
  public void deleteDirectory() throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).forEach(AllureEnvironmentListenerTest::delete);
    }
  }

  @Test
  public void entriesHoldTheEnvironmentNonSecretConfigurationAndRuntimeOnly() {
    Map<String, String> entries = listener(TestConfig::current).entries();

    assertThat(entries)
        .containsOnlyKeys(
            "test.environment",
            "report.project",
            "report.owner",
            "fixture.mirrored-value",
            "java.version",
            "os.name",
            "os.arch")
        .containsEntry("test.environment", "local")
        .containsEntry("report.project", "sdet-automation")
        .containsEntry("java.version", System.getProperty("java.version"));
  }

  @Test
  public void secretsNeverReachTheEnvironmentEvenWhenNonSecretKeysHoldTheirValue() {
    Map<String, String> entries = listener(TestConfig::current).entries();

    assertThat(entries).containsEntry("fixture.mirrored-value", MASK);
    assertThat(entries.keySet()).noneMatch(key -> key.startsWith("secret."));
    assertThat(String.join("\n", entries.values())).doesNotContain(SECRET);
  }

  @Test
  public void noSystemPropertiesUserDetailsOrPathsAreIncluded() {
    Map<String, String> entries = listener(TestConfig::current).entries();

    assertThat(entries.keySet())
        .doesNotContain(
            "user.name",
            "user.home",
            "user.dir",
            "java.home",
            "java.class.path",
            "allure.results.directory");
    String values = String.join("\n", entries.values());
    assertThat(values)
        .doesNotContain(System.getProperty("user.home"))
        .doesNotContain(System.getProperty("user.dir"));
  }

  @Test
  public void theFileIsSortedAndReadableAsProperties() throws IOException {
    AllureEnvironmentListener listener = listener(TestConfig::current);

    listener.onExecutionStart();

    Path file = directory.resolve(AllureEnvironmentListener.FILE_NAME);
    String content = Files.readString(file, StandardCharsets.ISO_8859_1);
    assertThat(load(file)).isEqualTo(listener.entries());
    assertThat(content).doesNotContain(SECRET);
    List<String> keys = content.lines().map(line -> line.substring(0, line.indexOf('='))).toList();
    assertThat(keys).isSorted().doesNotHaveDuplicates();
  }

  @Test
  public void repeatedRunsReplaceTheFileWithoutDuplicatesOrLeftovers() throws IOException {
    AllureEnvironmentListener listener = listener(TestConfig::current);
    Path file = directory.resolve(AllureEnvironmentListener.FILE_NAME);

    listener.onExecutionStart();
    String first = Files.readString(file, StandardCharsets.ISO_8859_1);
    listener.onExecutionStart();
    listener.onExecutionStart();

    assertThat(Files.readString(file, StandardCharsets.ISO_8859_1)).isEqualTo(first);
    try (Stream<Path> files = Files.list(directory)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  public void missingConfigurationIsReportedWithoutItsMessage() throws IOException {
    AllureEnvironmentListener listener =
        listener(
            () -> {
              throw new ConfigException("cannot load configuration containing " + SECRET);
            });

    try (LogCapture logs = new LogCapture(AllureEnvironmentListener.class)) {
      listener.onExecutionStart();

      assertThat(logs.messages())
          .singleElement()
          .asString()
          .contains("could not be loaded (ConfigException)");
      assertThat(logs.everything()).doesNotContain(SECRET);
    }
    assertThat(load(directory.resolve(AllureEnvironmentListener.FILE_NAME)))
        .containsOnlyKeys("test.environment", "java.version", "os.name", "os.arch")
        .containsEntry("test.environment", AllureEnvironmentListener.UNAVAILABLE);
  }

  @Test
  public void anUnwritableResultsDirectoryIsLoggedNotThrown() throws IOException {
    Path occupiedPath = Files.createFile(directory.resolve("occupied"));
    AllureEnvironmentListener listener =
        new AllureEnvironmentListener(TestConfig::current, () -> occupiedPath);

    try (LogCapture logs = new LogCapture(AllureEnvironmentListener.class)) {
      assertThatCode(listener::onExecutionStart).doesNotThrowAnyException();

      assertThat(logs.messages())
          .singleElement()
          .asString()
          .startsWith("Could not write Allure environment.properties (");
      assertThat(logs.everything()).doesNotContain(SECRET);
    }
  }

  @Test
  public void specialCharactersRoundTripThroughThePropertiesFormat() throws IOException {
    Map<String, String> entries = new TreeMap<>();
    entries.put("key with spaces=and:separators#!", "value = with : separators # !");
    entries.put("leading", "  two leading spaces");
    entries.put("multi", "line one\nline two\r\ttabbed\fform-feed");
    entries.put("unicode", "пароль 密码 🔑 ü");
    entries.put("backslash", "C:\\path\\to");
    entries.put("empty", "");

    listener(TestConfig::current).write(entries);

    assertThat(load(directory.resolve(AllureEnvironmentListener.FILE_NAME))).isEqualTo(entries);
  }

  @Test
  public void defaultResultsDirectoryIsTheModuleTargetDirectory() {
    assertThat(AllureEnvironmentListener.configuredResultsDirectory())
        .as("Surefire sets allure.results.directory to ${project.build.directory}/allure-results")
        .isEqualTo(Path.of("target", "allure-results").toAbsolutePath());
  }

  private AllureEnvironmentListener listener(Supplier<TestConfig> config) {
    return new AllureEnvironmentListener(config, () -> directory);
  }

  private static Map<String, String> load(Path file) throws IOException {
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
      properties.load(reader);
    }
    Map<String, String> loaded = new TreeMap<>();
    properties
        .stringPropertyNames()
        .forEach(name -> loaded.put(name, properties.getProperty(name)));
    return loaded;
  }

  private static void delete(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
