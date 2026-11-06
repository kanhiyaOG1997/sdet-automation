package io.github.kanhiyaog1997.sdet.reporting;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Sample with a failing data provider, run programmatically by the Allure masking tests. Allure
 * records this as a run-level error ({@code *-globals.json}) with the exception's stack trace.
 * Surefire never collects this class.
 */
public class DataProviderLeakSample {

  /**
   * Fails with the fake secret in its message, so the dependent test never runs.
   *
   * @return never returns
   */
  @DataProvider
  public Object[][] failingProvider() {
    throw new IllegalStateException("data provider failed with " + AllureLeakSample.SECRET);
  }

  @Test(dataProvider = "failingProvider")
  public void neverReceivesData(String value) {
    if (value.isEmpty()) {
      throw new AssertionError("unexpected empty value");
    }
  }
}
