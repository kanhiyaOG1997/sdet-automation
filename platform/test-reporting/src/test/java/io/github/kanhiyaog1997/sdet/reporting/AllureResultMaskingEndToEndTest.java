package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.TestNG;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs the leak samples through real TestNG, real ServiceLoader discovery and Allure's real file
 * writer, then reads the result, container and run-level error files back from {@code
 * target/allure-results}.
 *
 * <p>The samples include a failing configuration method and a failing data provider, both of which
 * make Allure write {@code *-globals.json}. Afterwards the files are moved, not deleted, to {@value
 * #EVIDENCE}, so they stay out of the module's own Allure report but remain available to inspect
 * and scan.
 */
public class AllureResultMaskingEndToEndTest {

  private static final String SECRET = AllureLeakSample.SECRET;
  private static final ObjectMapper JSON = new ObjectMapper();
  static final String EVIDENCE = "target/allure-sample-results/end-to-end";

  private final Path results = Path.of("target", "allure-results").toAbsolutePath();
  private final List<Path> created = new ArrayList<>();

  /** Runs both samples once with the default, ServiceLoader-configured Allure lifecycle. */
  @BeforeClass
  public void runSamples() {
    TestNG testng = new TestNG(false);
    testng.setVerbose(0);
    testng.setTestClasses(
        new Class<?>[] {
          AllureLeakSample.class, FixtureLeakSample.class, DataProviderLeakSample.class
        });
    Set<Path> before = filesIn(results);
    testng.run();
    for (Path file : filesIn(results)) {
      if (!before.contains(file)) {
        created.add(file);
      }
    }
  }

  /** Moves the sample's files out of the module's Allure results, keeping them as evidence. */
  @AfterClass(alwaysRun = true)
  public void moveSampleFilesToEvidence() throws IOException {
    Path evidence = Files.createDirectories(Path.of(EVIDENCE).toAbsolutePath());
    for (Path file : created) {
      Files.move(file, evidence.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Test
  public void resultAndContainerFilesWereWritten() {
    assertThat(created)
        .anyMatch(file -> file.getFileName().toString().endsWith("-result.json"))
        .anyMatch(file -> file.getFileName().toString().endsWith("-container.json"));
    assertThat(filesEndingWith("-globals.json"))
        .as("one file per configuration failure and per data-provider failure")
        .hasSize(2);
  }

  @Test
  public void noFileWrittenByTheRunContainsTheSecret() throws IOException {
    assertThat(created).isNotEmpty();
    for (Path file : created) {
      assertThat(Files.readString(file, StandardCharsets.ISO_8859_1))
          .as(file.getFileName().toString())
          .doesNotContain(SECRET);
    }
  }

  @Test
  public void theFilesOnDiskHoldMaskedValuesRatherThanNothing() throws IOException {
    JsonNode failed = resultFile("failsWithSecretEverywhere");

    assertThat(failed.path("statusDetails").path("message").asText())
        .isEqualTo("expected " + MASK + " but was nothing");
    assertThat(failed.path("statusDetails").path("trace").asText())
        .contains("Caused by: java.lang.IllegalStateException: root cause carrying " + MASK);
    assertThat(
            resultFile("receivesSecretParameter").path("parameters").get(0).path("value").asText())
        .isEqualTo(MASK);
    assertThat(containerText()).contains("setup failed with " + MASK);
  }

  @Test
  public void globalsFilesOnDiskHoldMaskedFailureDetails() throws IOException {
    StringBuilder globals = new StringBuilder();
    for (Path file : filesEndingWith("-globals.json")) {
      JsonNode error = JSON.readTree(file.toFile()).path("errors").get(0);
      assertThat(error.path("timestamp").asLong()).isPositive();
      globals.append(error.path("message").asText()).append('\n');
      globals.append(error.path("trace").asText()).append('\n');
    }

    assertThat(globals.toString())
        .contains("FixtureLeakSample.failingSetUp")
        .contains("setup failed with " + MASK)
        .contains("DataProviderLeakSample.neverReceivesData")
        .contains("data provider failed with " + MASK)
        .doesNotContain(SECRET);
  }

  // ---- helpers ----

  private JsonNode resultFile(String name) throws IOException {
    for (Path file : created) {
      if (file.getFileName().toString().endsWith("-result.json")) {
        JsonNode node = JSON.readTree(file.toFile());
        if (node.path("name").asText().equals(name)) {
          return node;
        }
      }
    }
    throw new AssertionError("no result file for " + name);
  }

  private List<Path> filesEndingWith(String suffix) {
    return created.stream().filter(file -> file.getFileName().toString().endsWith(suffix)).toList();
  }

  private String containerText() throws IOException {
    StringBuilder text = new StringBuilder();
    for (Path file : created) {
      if (file.getFileName().toString().endsWith("-container.json")) {
        text.append(Files.readString(file, StandardCharsets.UTF_8));
      }
    }
    return text.toString();
  }

  private static Set<Path> filesIn(Path directory) {
    if (!Files.isDirectory(directory)) {
      return Set.of();
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files.collect(Collectors.toCollection(HashSet::new));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
