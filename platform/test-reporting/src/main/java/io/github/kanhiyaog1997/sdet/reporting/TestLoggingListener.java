package io.github.kanhiyaog1997.sdet.reporting;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Logs the start and outcome of every TestNG test, and puts the test name into the SLF4J MDC under
 * {@value #MDC_KEY} so log lines from parallel tests can be told apart.
 *
 * <p>What is logged: the test name, the number of parameters (never their values), the duration,
 * and for failures and skips the exception type plus the first line of its message, masked by
 * {@link SensitiveDataMasker} and then truncated. Stack traces, request and response bodies and
 * parameter values are never logged.
 *
 * <p>Registered automatically through {@code META-INF/services/org.testng.ITestNGListener}.
 */
public final class TestLoggingListener implements ITestListener {

  /** MDC key that holds the running test's name. */
  public static final String MDC_KEY = "testName";

  static final int MAX_MESSAGE_LENGTH = 300;

  private static final Logger LOG = LoggerFactory.getLogger(TestLoggingListener.class);

  private final Supplier<SensitiveDataMasker> masker;

  /** Creates the listener used by TestNG, masking with the secrets of the current run. */
  public TestLoggingListener() {
    this(SensitiveDataMasker::forCurrentRun);
  }

  TestLoggingListener(Supplier<SensitiveDataMasker> masker) {
    this.masker = masker;
  }

  /** Logs the start of a test and records its name in the MDC. */
  @Override
  public void onTestStart(ITestResult result) {
    String name = name(result);
    MDC.put(MDC_KEY, name);
    LOG.info("START {}{}", name, parameters(result));
  }

  /** Logs a passed test. */
  @Override
  public void onTestSuccess(ITestResult result) {
    LOG.info("PASS  {} ({} ms)", name(result), duration(result));
    MDC.remove(MDC_KEY);
  }

  /** Logs a failed test with its masked failure reason. */
  @Override
  public void onTestFailure(ITestResult result) {
    LOG.warn(
        "FAIL  {} ({} ms): {}", name(result), duration(result), describe(result.getThrowable()));
    MDC.remove(MDC_KEY);
  }

  /** Logs a skipped test with its masked skip reason. */
  @Override
  public void onTestSkipped(ITestResult result) {
    LOG.info("SKIP  {}: {}", name(result), describe(result.getThrowable()));
    MDC.remove(MDC_KEY);
  }

  /** Logs a failure that is still within the test's success percentage. */
  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    LOG.warn(
        "FAIL  {} ({} ms, within success percentage): {}",
        name(result),
        duration(result),
        describe(result.getThrowable()));
    MDC.remove(MDC_KEY);
  }

  String describe(Throwable throwable) {
    if (throwable == null) {
      return "no exception";
    }
    String type = throwable.getClass().getSimpleName();
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return type;
    }
    String masked = masker.get().mask(message.strip());
    int lineEnd = masked.indexOf('\n');
    String firstLine = lineEnd < 0 ? masked : masked.substring(0, lineEnd).strip();
    if (firstLine.length() > MAX_MESSAGE_LENGTH) {
      firstLine = firstLine.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }
    return type + ": " + firstLine;
  }

  private static String name(ITestResult result) {
    return result.getTestClass().getRealClass().getSimpleName() + "." + result.getName();
  }

  private static String parameters(ITestResult result) {
    Object[] parameters = result.getParameters();
    int count = parameters == null ? 0 : parameters.length;
    return count == 0 ? "" : " with " + count + " parameter(s)";
  }

  private static long duration(ITestResult result) {
    return Math.max(0, result.getEndMillis() - result.getStartMillis());
  }
}
