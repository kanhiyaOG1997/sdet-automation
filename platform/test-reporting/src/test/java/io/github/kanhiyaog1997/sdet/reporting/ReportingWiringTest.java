package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.testng.AllureTestNg;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.TestNG;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * End-to-end check: real ServiceLoader discovery, real Allure listener, real results directory.
 * Everything is asserted on the files Allure actually wrote, which proves that masking happens
 * before the attachments are created.
 */
public class ReportingWiringTest {

  private static final String SECRET = ReportingWiringSample.SECRET;
  private static final ObjectMapper JSON = new ObjectMapper();

  private final Path results = Path.of("target", "allure-results").toAbsolutePath();
  private final List<Path> createdBySample = new ArrayList<>();
  private TestNG testng;
  private List<String> logLines;
  private JsonNode sampleResult;

  /** Runs the sample once, with ServiceLoader discovery plus one explicit duplicate listener. */
  @BeforeClass
  public void runSample() throws IOException {
    testng = new TestNG(false);
    testng.setVerbose(0);
    testng.setTestClasses(new Class<?>[] {ReportingWiringSample.class});
    testng.addListener(new TestLoggingListener());
    Set<Path> before = filesIn(results);
    try (LogCapture logs = new LogCapture(TestLoggingListener.class)) {
      testng.run();
      logLines = logs.messages();
    }
    for (Path file : filesIn(results)) {
      if (!before.contains(file)) {
        createdBySample.add(file);
      }
    }
    sampleResult = sampleResult();
  }

  /** Moves the sample's files out of the module's Allure results, keeping them as evidence. */
  @AfterClass(alwaysRun = true)
  public void moveSampleResultsToEvidence() throws IOException {
    Path evidence =
        Files.createDirectories(Path.of("target/allure-sample-results/wiring").toAbsolutePath());
    for (Path file : createdBySample) {
      Files.move(file, evidence.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Test
  public void resultsAreWrittenToExactlyTheConfiguredTargetDirectory() {
    assertThat(Path.of(System.getProperty("allure.results.directory"))).isEqualTo(results);
    assertThat(Path.of("allure-results")).as("Allure's fallback location").doesNotExist();
    assertThat(createdBySample).isNotEmpty().allMatch(file -> file.getParent().equals(results));
    assertThat(results.resolve(AllureEnvironmentListener.FILE_NAME)).isRegularFile();
  }

  @Test
  public void eachListenerIsRegisteredExactlyOnceThroughServiceLoader() {
    Map<Class<?>, Long> registered =
        testng.getServiceLoaderListeners().stream()
            .collect(Collectors.groupingBy(Object::getClass, Collectors.counting()));

    assertThat(registered)
        .containsEntry(AllureTestNg.class, 1L)
        .containsEntry(AllureLifecycleInstaller.class, 1L)
        .containsEntry(AllureEnvironmentListener.class, 1L)
        .containsEntry(TestLoggingListener.class, 1L);
  }

  @Test
  public void allureTestNgWritesThroughTheMaskingLifecycle() {
    AllureTestNg allure =
        testng.getServiceLoaderListeners().stream()
            .filter(AllureTestNg.class::isInstance)
            .map(AllureTestNg.class::cast)
            .findFirst()
            .orElseThrow();

    assertThat(AllureLifecycleInstaller.installed()).isNotNull();
    assertThat(allure.getLifecycle()).isSameAs(AllureLifecycleInstaller.installed());
    assertThat(Allure.getLifecycle()).isSameAs(AllureLifecycleInstaller.installed());
  }

  @Test
  public void anExplicitlyAddedDuplicateListenerDoesNotDuplicateLogLines() {
    assertThat(logLines)
        .filteredOn(line -> line.startsWith("START ReportingWiringSample.attachesSensitiveContent"))
        .hasSize(1);
    assertThat(logLines)
        .filteredOn(line -> line.startsWith("PASS  ReportingWiringSample.attachesSensitiveContent"))
        .hasSize(1);
  }

  @Test
  public void theSamplePassedAndProducedOneResultWithFiveAttachments() {
    assertThat(testng.getStatus()).isZero();
    assertThat(sampleResult.path("status").asText()).isEqualTo("passed");
    assertThat(attachments()).hasSize(5);
  }

  @Test
  public void textAttachmentsAreMaskedBeforeTheyAreWritten() throws IOException {
    JsonNode request = attachmentNamed("request for " + MASK);

    assertThat(request.path("type").asText()).isEqualTo("text/plain");
    assertThat(read(request))
        .contains("GET /users HTTP/1.1", "Authorization: " + MASK, "body contains " + MASK)
        .doesNotContain(SECRET, "abcdefghijkl");
  }

  @Test
  public void jsonAttachmentsArePrettyPrintedThenMasked() throws IOException {
    JsonNode response = attachmentNamed("response");

    assertThat(response.path("type").asText()).isEqualTo("application/json");
    assertThat(response.path("source").asText()).endsWith(".json");
    assertThat(read(response))
        .contains("\"user\" : \"alice\"")
        .contains("\"password\" : \"" + MASK + "\"")
        .contains("\"echo\" : \"" + MASK + "\"")
        .doesNotContain(SECRET, "hunter22");
  }

  @Test
  public void invalidJsonIsAttachedAsMaskedText() throws IOException {
    JsonNode notJson = attachmentNamed("not json");

    assertThat(notJson.path("type").asText()).isEqualTo("text/plain");
    assertThat(read(notJson)).isEqualTo("token=" + MASK + " {broken");
  }

  @Test
  public void nullNameAndContentBecomeDefaultNamedEmptyAttachment() throws IOException {
    assertThat(read(attachmentNamed(ReportAttachments.DEFAULT_NAME))).isEmpty();
  }

  @Test
  public void imagesAreAttachedUnchangedButTheirNamesAreMasked() throws IOException {
    JsonNode screenshot = attachmentNamed("screenshot " + MASK);

    assertThat(screenshot.path("type").asText()).isEqualTo("image/png");
    assertThat(Files.readAllBytes(results.resolve(screenshot.path("source").asText())))
        .isEqualTo(ReportingWiringSample.PNG);
  }

  @Test
  public void noFileWrittenByTheRunContainsTheSecret() throws IOException {
    assertThat(createdBySample).isNotEmpty();
    for (Path file : createdBySample) {
      assertThat(Files.readString(file, StandardCharsets.ISO_8859_1))
          .as(file.getFileName().toString())
          .doesNotContain(SECRET);
    }
    assertThat(Files.readString(results.resolve(AllureEnvironmentListener.FILE_NAME)))
        .doesNotContain(SECRET);
  }

  // ---- helpers ----

  private JsonNode sampleResult() throws IOException {
    List<JsonNode> matches = new ArrayList<>();
    for (Path file : createdBySample) {
      if (file.getFileName().toString().endsWith("-result.json")) {
        JsonNode node = JSON.readTree(file.toFile());
        if (node.path("name").asText().equals("attachesSensitiveContent")) {
          matches.add(node);
        }
      }
    }
    assertThat(matches).hasSize(1);
    return matches.get(0);
  }

  /** Attachments of the result and of all its steps; Allure 3 records each one as a step. */
  private List<JsonNode> attachments() {
    List<JsonNode> found = new ArrayList<>();
    collectAttachments(sampleResult, found);
    return found;
  }

  private static void collectAttachments(JsonNode node, List<JsonNode> found) {
    node.path("attachments").forEach(found::add);
    node.path("steps").forEach(step -> collectAttachments(step, found));
  }

  private JsonNode attachmentNamed(String name) {
    Map<String, JsonNode> byName =
        attachments().stream()
            .collect(Collectors.toMap(a -> a.path("name").asText(), Function.identity()));
    assertThat(byName).as("attachment names").containsKey(name);
    return byName.get(name);
  }

  private String read(JsonNode attachment) throws IOException {
    return Files.readString(
        results.resolve(attachment.path("source").asText()), StandardCharsets.UTF_8);
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
