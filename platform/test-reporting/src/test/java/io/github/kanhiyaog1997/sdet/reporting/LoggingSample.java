package io.github.kanhiyaog1997.sdet.reporting;

import java.util.concurrent.atomic.AtomicInteger;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Sample tests that {@link TestLoggingListenerTest} runs through a separate, programmatic TestNG
 * run. Surefire never collects this class: its name does not match any test-class pattern. Some of
 * these tests fail or skip on purpose, with the fake secret in their messages.
 */
public class LoggingSample {

  static final String SECRET = "TASK5-SECRET-12345";

  private final AtomicInteger flakyInvocations = new AtomicInteger();

  @Test
  public void passes() {}

  @Test
  public void failsWithSecretInMessage() {
    throw new AssertionError(
        "expected token " + SECRET + " but got Bearer abcdefghijklmnop\nsecond line " + SECRET);
  }

  @Test
  public void failsWithLongMessageCutAcrossTheSecret() {
    throw new IllegalStateException(
        "x".repeat(TestLoggingListener.MAX_MESSAGE_LENGTH - 5) + SECRET + " tail");
  }

  @Test
  public void failsWithoutMessage() {
    throw new IllegalStateException();
  }

  @Test
  public void skipsWithSecretInReason() {
    throw new SkipException("skipped because " + SECRET + " is not accepted");
  }

  /**
   * Supplies the fake secret as a test parameter.
   *
   * @return one row containing the secret
   */
  @DataProvider
  public Object[][] credentials() {
    return new Object[][] {{SECRET}};
  }

  @Test(dataProvider = "credentials")
  public void usesSecretParameter(String credential) {
    if (credential.isEmpty()) {
      throw new AssertionError("unexpected empty credential");
    }
  }

  @Test(invocationCount = 2, successPercentage = 50)
  public void failsOnceWithinSuccessPercentage() {
    if (flakyInvocations.incrementAndGet() == 1) {
      throw new AssertionError("flaky with " + SECRET);
    }
  }
}
