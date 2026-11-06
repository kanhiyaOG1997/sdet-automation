package io.github.kanhiyaog1997.sdet.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** TEMPORARY O3 failure-path probe. Fails on purpose; never merge. */
public class CiFailurePathProbeTest {

  @Test
  public void deliberateFailureForCiValidation() {
    assertThat("O3 deliberate failure").isEqualTo("expected to fail");
  }
}
