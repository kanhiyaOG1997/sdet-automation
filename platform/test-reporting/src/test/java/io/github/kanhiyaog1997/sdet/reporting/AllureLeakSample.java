package io.github.kanhiyaog1997.sdet.reporting;

import io.qameta.allure.Allure;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Sample tests run programmatically by the Allure masking tests. Each one puts the fake secret into
 * a different place where Allure records data by itself. Surefire never collects this class.
 */
public class AllureLeakSample {

  static final String SECRET = "TASK5-SECRET-12345";

  @Test
  public void failsWithSecretEverywhere() {
    Allure.description("description with " + SECRET);
    Allure.link("docs " + SECRET, "https://docs.example.test/page?token=" + SECRET);
    Allure.parameter("credential", SECRET);
    Allure.step("log in as admin with " + SECRET, () -> {});
    Allure.step("nested parent", () -> Allure.step("nested child " + SECRET));
    throw new AssertionError(
        "expected " + SECRET + " but was nothing",
        new IllegalStateException("root cause carrying " + SECRET));
  }

  @Test
  public void skipsWithSecretInReason() {
    throw new SkipException("skipped because of " + SECRET);
  }

  /**
   * Supplies the fake secret as a data-provider parameter, which must never be done in real suites;
   * it is here only to prove that the value is still masked.
   *
   * @return one row containing the secret
   */
  @DataProvider
  public Object[][] secrets() {
    return new Object[][] {{SECRET}};
  }

  @Test(dataProvider = "secrets")
  public void receivesSecretParameter(String value) {
    if (value.isEmpty()) {
      throw new AssertionError("unexpected empty value");
    }
  }
}
