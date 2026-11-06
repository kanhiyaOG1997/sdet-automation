package io.github.kanhiyaog1997.sdet.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.qameta.allure.testng.AllureTestNg;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.MDC;
import org.testng.TestNG;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs {@link LoggingSample} through a real TestNG run and checks exactly what the listener logs.
 */
public class TestLoggingListenerTest {

  private static final String SECRET = LoggingSample.SECRET;

  private List<ILoggingEvent> events;
  private List<String> messages;
  private String everything;
  private String mdcAfterRun;

  /** Runs the sample once with only an explicitly configured logging listener. */
  @BeforeClass
  public void runSample() {
    TestNG testng = new TestNG(false);
    testng.setVerbose(0);
    testng.setTestClasses(new Class<?>[] {LoggingSample.class});
    testng.setListenersToSkipFromBeingWiredInViaServiceLoaders(
        AllureTestNg.class.getName(),
        AllureEnvironmentListener.class.getName(),
        TestLoggingListener.class.getName());
    testng.addListener(new TestLoggingListener(() -> new SensitiveDataMasker(List.of(SECRET))));
    try (LogCapture logs = new LogCapture(TestLoggingListener.class)) {
      testng.run();
      events = logs.events();
      messages = logs.messages();
      everything = logs.everything();
    }
    mdcAfterRun = MDC.get(TestLoggingListener.MDC_KEY);
  }

  @Test
  public void everyTestIsLoggedOnceAtStartAndOnceWithItsOutcome() {
    for (String test :
        List.of(
            "passes",
            "failsWithSecretInMessage",
            "failsWithLongMessageCutAcrossTheSecret",
            "failsWithoutMessage",
            "skipsWithSecretInReason",
            "usesSecretParameter")) {
      String name = "LoggingSample." + test;
      assertThat(messages).filteredOn(startsWith("START " + name)).as(test).hasSize(1);
      assertThat(messages)
          .filteredOn(m -> m.matches("(PASS|FAIL|SKIP) {2}" + name + "\\b.*"))
          .as(test)
          .hasSize(1);
    }
    assertThat(messages)
        .filteredOn(startsWith("START LoggingSample.failsOnceWithinSuccessPercentage"))
        .hasSize(2);
    assertThat(messages).hasSize(16);
  }

  @Test
  public void outcomesUseTheExpectedLevelsAndFormat() {
    assertThat(line("PASS  LoggingSample.passes"))
        .matches("PASS {2}LoggingSample\\.passes \\(\\d+ ms\\)");
    assertThat(level("FAIL  LoggingSample.failsWithoutMessage")).isEqualTo(Level.WARN);
    assertThat(level("PASS  LoggingSample.passes")).isEqualTo(Level.INFO);
    assertThat(line("FAIL  LoggingSample.failsWithoutMessage"))
        .endsWith("): IllegalStateException");
  }

  @Test
  public void failureMessagesAreMaskedAndCutToTheirFirstLine() {
    assertThat(line("FAIL  LoggingSample.failsWithSecretInMessage"))
        .endsWith("): AssertionError: expected token **** but got Bearer ****")
        .doesNotContain("second line");
  }

  @Test
  public void longMessagesAreMaskedBeforeTheyAreTruncated() {
    String failure = line("FAIL  LoggingSample.failsWithLongMessageCutAcrossTheSecret");

    assertThat(failure).endsWith("...").contains("****").doesNotContain("TASK5");
  }

  @Test
  public void failuresWithinTheSuccessPercentageAreLoggedAndMasked() {
    assertThat(line("FAIL  LoggingSample.failsOnceWithinSuccessPercentage"))
        .contains("within success percentage")
        .endsWith("): AssertionError: flaky with ****");
    assertThat(level("FAIL  LoggingSample.failsOnceWithinSuccessPercentage")).isEqualTo(Level.WARN);
    assertThat(line("PASS  LoggingSample.failsOnceWithinSuccessPercentage")).isNotEmpty();
  }

  @Test
  public void skipReasonsAreMasked() {
    assertThat(line("SKIP  LoggingSample.skipsWithSecretInReason"))
        .isEqualTo(
            "SKIP  LoggingSample.skipsWithSecretInReason: SkipException: skipped because **** is"
                + " not accepted");
  }

  @Test
  public void parameterValuesAreNeverLoggedOnlyTheirCount() {
    assertThat(line("START LoggingSample.usesSecretParameter"))
        .isEqualTo("START LoggingSample.usesSecretParameter with 1 parameter(s)");
  }

  @Test
  public void nothingTheListenerLogsContainsTheSecretOrStackTraces() {
    assertThat(everything).doesNotContain(SECRET).doesNotContain("abcdefghijklmnop");
    assertThat(events).allSatisfy(event -> assertThat(event.getThrowableProxy()).isNull());
  }

  @Test
  public void theTestNameIsInTheMdcWhileTheTestRunsAndRemovedAfterwards() {
    assertThat(events)
        .filteredOn(event -> event.getFormattedMessage().startsWith("START LoggingSample.passes"))
        .singleElement()
        .satisfies(
            event ->
                assertThat(event.getMDCPropertyMap())
                    .containsEntry(TestLoggingListener.MDC_KEY, "LoggingSample.passes"));
    assertThat(mdcAfterRun).as("MDC right after the sample run").isNull();
  }

  @Test
  public void describeHandlesMissingExceptionsAndMessages() {
    TestLoggingListener listener =
        new TestLoggingListener(() -> new SensitiveDataMasker(List.of()));

    assertThat(listener.describe(null)).isEqualTo("no exception");
    assertThat(listener.describe(new IllegalStateException("   ")))
        .isEqualTo("IllegalStateException");
    assertThat(listener.describe(new IllegalStateException("password=abc")))
        .isEqualTo("IllegalStateException: password=****");
  }

  private String line(String prefix) {
    return messages.stream().filter(startsWith(prefix)).findFirst().orElseThrow();
  }

  private Level level(String prefix) {
    return events.stream()
        .filter(event -> event.getFormattedMessage().startsWith(prefix))
        .findFirst()
        .orElseThrow()
        .getLevel();
  }

  private static Predicate<String> startsWith(String prefix) {
    return message -> message.startsWith(prefix);
  }
}
