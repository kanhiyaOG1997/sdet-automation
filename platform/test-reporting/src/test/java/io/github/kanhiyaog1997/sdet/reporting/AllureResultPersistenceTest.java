package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.testng.AllureTestNg;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.testng.TestNG;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Proves masking happens before persistence: the samples run against an {@link AllureLifecycle}
 * whose writer serialises each result, container and run-level error at the moment Allure writes
 * it. The lifecycle is built with Allure's default ServiceLoader notifier, so the real {@link
 * AllureResultMasker} registration is what is exercised, and its writer is the {@link
 * MaskingResultsWriter} that {@link AllureLifecycleInstaller} installs, wrapping the recorder.
 */
public class AllureResultPersistenceTest {

  private static final String SECRET = AllureLeakSample.SECRET;
  private static final ObjectMapper JSON = new ObjectMapper();

  private final RecordingResultsWriter writer = new RecordingResultsWriter();
  private final List<JsonNode> results = new ArrayList<>();
  private final List<JsonNode> containers = new ArrayList<>();
  private final List<JsonNode> globals = new ArrayList<>();

  /** Runs both samples once against a recording lifecycle, restoring Allure's global lifecycle. */
  @BeforeClass
  public void runSamplesAgainstRecordingWriter() {
    AllureLifecycle recording =
        new AllureLifecycle(new MaskingResultsWriter(writer, new AllureResultMasker()));
    AllureLifecycle previous = Allure.getLifecycle();
    Allure.setLifecycle(recording);
    try {
      TestNG testng = new TestNG(false);
      testng.setVerbose(0);
      testng.setTestClasses(
          new Class<?>[] {
            AllureLeakSample.class, FixtureLeakSample.class, DataProviderLeakSample.class
          });
      testng.setListenersToSkipFromBeingWiredInViaServiceLoaders(
          AllureTestNg.class.getName(),
          AllureLifecycleInstaller.class.getName(),
          AllureEnvironmentListener.class.getName(),
          TestLoggingListener.class.getName());
      testng.addListener(new AllureTestNg(recording));
      testng.run();
    } finally {
      Allure.setLifecycle(previous);
    }
    writer.results().forEach(json -> results.add(parse(json)));
    writer.containers().forEach(json -> containers.add(parse(json)));
    writer.globals().forEach(json -> globals.add(parse(json)));
  }

  @Test
  public void everyResultAndContainerWasRecordedAtWriteTime() {
    assertThat(results)
        .extracting(node -> node.path("name").asText())
        .contains(
            "failsWithSecretEverywhere",
            "skipsWithSecretInReason",
            "receivesSecretParameter",
            "neverRuns");
    assertThat(containers).isNotEmpty();
  }

  @Test
  public void noPersistedResultContainerGlobalsOrAttachmentContainsTheSecret() {
    assertThat(writer.results()).allSatisfy(json -> assertThat(json).doesNotContain(SECRET));
    assertThat(writer.containers()).allSatisfy(json -> assertThat(json).doesNotContain(SECRET));
    assertThat(writer.globals())
        .isNotEmpty()
        .allSatisfy(json -> assertThat(json).doesNotContain(SECRET));
    assertThat(writer.attachments().values())
        .allSatisfy(
            bytes -> assertThat(new String(bytes, StandardCharsets.UTF_8)).doesNotContain(SECRET));
  }

  @Test
  public void failureDetailsAreMaskedNotRemoved() {
    JsonNode failed = result("failsWithSecretEverywhere");
    JsonNode details = failed.path("statusDetails");

    assertThat(failed.path("status").asText()).isEqualToIgnoringCase("failed");
    assertThat(details.path("message").asText()).isEqualTo("expected " + MASK + " but was nothing");
    assertThat(details.path("trace").asText())
        .contains("java.lang.AssertionError: expected " + MASK)
        .contains("Caused by: java.lang.IllegalStateException: root cause carrying " + MASK)
        .contains("at io.github.kanhiyaog1997.sdet.reporting.AllureLeakSample");
  }

  @Test
  public void stepsLinksDescriptionsAndParametersAreMasked() {
    JsonNode failed = result("failsWithSecretEverywhere");

    assertThat(failed.path("description").asText()).isEqualTo("description with " + MASK);
    assertThat(failed.toString())
        .contains("log in as admin with " + MASK)
        .contains("nested child " + MASK)
        .contains("https://docs.example.test/page?token=" + MASK);
    JsonNode credential = parameter(failed, "credential");
    assertThat(credential.path("value").asText()).isEqualTo(MASK);
    assertThat(credential.path("mode").asText()).isEqualToIgnoringCase("masked");
  }

  @Test
  public void dataProviderValuesAreMaskedButHistoryIdIsKept() {
    JsonNode parameterised = result("receivesSecretParameter");

    JsonNode value = parameterised.path("parameters").get(0);
    assertThat(value.path("value").asText()).isEqualTo(MASK);
    assertThat(value.path("mode").asText()).isEqualToIgnoringCase("masked");
    assertThat(parameterised.path("historyId").asText()).isNotBlank();
  }

  @Test
  public void skipReasonsAreMasked() {
    assertThat(result("skipsWithSecretInReason").path("statusDetails").path("message").asText())
        .isEqualTo("skipped because of " + MASK);
  }

  @Test
  public void fixtureFailuresInContainersAreMaskedAndRelationshipsKept() {
    JsonNode fixture =
        containers.stream()
            .flatMap(container -> streamOf(container.path("befores")))
            .filter(before -> before.path("name").asText().equals("failingSetUp"))
            .findFirst()
            .orElseThrow();

    assertThat(fixture.path("statusDetails").path("message").asText())
        .isEqualTo("setup failed with " + MASK);
    String neverRuns = result("neverRuns").path("uuid").asText();
    assertThat(containers)
        .anySatisfy(
            container ->
                assertThat(container.path("children").toString())
                    .contains("\"" + neverRuns + "\""));
  }

  @Test
  public void configurationFailureGlobalErrorIsMaskedAtWriteTime() {
    JsonNode error = globalError("FixtureLeakSample.failingSetUp");

    assertThat(error.path("message").asText()).endsWith("setup failed with " + MASK);
    assertThat(error.path("trace").asText())
        .contains("java.lang.IllegalStateException: setup failed with " + MASK)
        .contains("at io.github.kanhiyaog1997.sdet.reporting.FixtureLeakSample");
    assertThat(error.path("timestamp").asLong()).isPositive();
  }

  @Test
  public void dataProviderFailureGlobalErrorIsMaskedAtWriteTime() {
    JsonNode error = globalError("DataProviderLeakSample.neverReceivesData");

    assertThat(error.path("trace").asText()).contains("data provider failed with " + MASK);
    assertThat(error.path("timestamp").asLong()).isPositive();
  }

  // ---- helpers ----

  private JsonNode globalError(String containing) {
    return globals.stream()
        .flatMap(node -> streamOf(node.path("errors")))
        .filter(error -> error.path("message").asText().contains(containing))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no global error mentioning " + containing));
  }

  private JsonNode result(String name) {
    return results.stream()
        .filter(node -> node.path("name").asText().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static JsonNode parameter(JsonNode result, String name) {
    return streamOf(result.path("parameters"))
        .filter(p -> p.path("name").asText().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static java.util.stream.Stream<JsonNode> streamOf(JsonNode array) {
    List<JsonNode> items = new ArrayList<>();
    array.forEach(items::add);
    return items.stream();
  }

  private static JsonNode parse(String json) {
    try {
      return JSON.readTree(json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
