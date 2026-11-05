package io.github.kanhiyaog1997.sdet.reporting;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Sample with a failing fixture, run programmatically by the Allure masking tests. Allure stores
 * fixture failures in result containers rather than in test results. Surefire never collects this
 * class.
 */
public class FixtureLeakSample {

  /** Fails with the fake secret in its message, so the dependent test is skipped. */
  @BeforeMethod
  public void failingSetUp() {
    throw new IllegalStateException("setup failed with " + AllureLeakSample.SECRET);
  }

  @Test
  public void neverRuns() {}
}
