package io.github.kanhiyaog1997.sdet.reporting;

import io.github.kanhiyaog1997.sdet.core.config.ConfigException;
import io.github.kanhiyaog1997.sdet.core.config.TestConfig;
import io.qameta.allure.util.PropertiesUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;

/**
 * Writes {@code environment.properties} into the Allure results directory when a TestNG run starts,
 * so the report shows which environment and configuration the tests ran against.
 *
 * <p>Contents: the environment name, the non-secret configuration from {@link
 * TestConfig#nonSecretEntries()} (values masked again as a second line of defence), the Java
 * version, and the operating system name and architecture. Raw system properties, environment
 * variables, user names and file-system paths are never included. If the configuration cannot be
 * loaded, the file is still written, with {@code test.environment=unavailable}.
 *
 * <p>Registered automatically through {@code META-INF/services/org.testng.ITestNGListener}. The
 * file is replaced on every run, so repeated runs never accumulate entries.
 */
public final class AllureEnvironmentListener implements IExecutionListener {

  static final String FILE_NAME = "environment.properties";
  static final String ENVIRONMENT_KEY = "test.environment";
  static final String UNAVAILABLE = "unavailable";

  private static final Logger LOG = LoggerFactory.getLogger(AllureEnvironmentListener.class);

  private final Supplier<TestConfig> config;
  private final Supplier<Path> resultsDirectory;

  /** Creates the listener used by TestNG, reading {@link TestConfig#current()}. */
  public AllureEnvironmentListener() {
    this(TestConfig::current, AllureEnvironmentListener::configuredResultsDirectory);
  }

  AllureEnvironmentListener(Supplier<TestConfig> config, Supplier<Path> resultsDirectory) {
    this.config = config;
    this.resultsDirectory = resultsDirectory;
  }

  /** Writes the environment file at the start of the TestNG run. */
  @Override
  public void onExecutionStart() {
    write(entries());
  }

  Map<String, String> entries() {
    Map<String, String> entries = new TreeMap<>();
    try {
      TestConfig current = config.get();
      SensitiveDataMasker masker = new SensitiveDataMasker(current.secretValues());
      current.nonSecretEntries().forEach((key, value) -> entries.put(key, masker.mask(value)));
      entries.put(ENVIRONMENT_KEY, current.environment());
    } catch (ConfigException e) {
      entries.put(ENVIRONMENT_KEY, UNAVAILABLE);
      LOG.warn(
          "Allure environment information omits the test configuration because it could not be"
              + " loaded ({})",
          e.getClass().getSimpleName());
    }
    entries.put("java.version", System.getProperty("java.version", ""));
    entries.put("os.name", System.getProperty("os.name", ""));
    entries.put("os.arch", System.getProperty("os.arch", ""));
    return entries;
  }

  void write(Map<String, String> entries) {
    StringBuilder content = new StringBuilder();
    entries.forEach(
        (key, value) ->
            content
                .append(escape(key, true))
                .append('=')
                .append(escape(value, false))
                .append('\n'));
    Path directory = resultsDirectory.get();
    Path temporary = null;
    try {
      Files.createDirectories(directory);
      temporary = Files.createTempFile(directory, ".environment-", ".tmp");
      Files.writeString(temporary, content, StandardCharsets.ISO_8859_1);
      moveIntoPlace(temporary, directory.resolve(FILE_NAME));
    } catch (IOException e) {
      LOG.warn("Could not write Allure {} ({})", FILE_NAME, e.getClass().getSimpleName());
      deleteQuietly(temporary);
    }
  }

  static Path configuredResultsDirectory() {
    return Paths.get(
        PropertiesUtils.loadAllureProperties()
            .getProperty("allure.results.directory", "allure-results"));
  }

  /**
   * Escapes text for a {@code .properties} file in the same way as {@link
   * java.util.Properties#store(java.io.OutputStream, String)}: backslash and control characters are
   * escaped, characters outside printable ASCII become {@code \}{@code uXXXX}, and keys also escape
   * separators and comment characters.
   */
  static String escape(String text, boolean isKey) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '\\' -> out.append("\\\\");
        case '\t' -> out.append("\\t");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\f' -> out.append("\\f");
        case ' ' -> out.append(isKey || i == 0 ? "\\ " : " ");
        case '=', ':', '#', '!' -> out.append(isKey ? "\\" + c : String.valueOf(c));
        default -> {
          if (c < 0x20 || c > 0x7e) {
            out.append(String.format("\\u%04X", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }

  private static void moveIntoPlace(Path source, Path target) throws IOException {
    try {
      Files.move(
          source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      LOG.debug("Could not delete temporary file ({})", e.getClass().getSimpleName());
    }
  }
}
