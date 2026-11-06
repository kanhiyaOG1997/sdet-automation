package io.github.kanhiyaog1997.sdet.reporting;

import org.testng.annotations.Test;

/**
 * Sample test that {@link ReportingWiringTest} runs through a separate, programmatic TestNG run
 * with the real ServiceLoader listeners and the real Allure results writer. Surefire never collects
 * this class: its name does not match any test-class pattern.
 */
public class ReportingWiringSample {

  static final String SECRET = "TASK5-SECRET-12345";
  static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

  @Test
  public void attachesSensitiveContent() {
    ReportAttachments.text(
        "request for " + SECRET,
        "GET /users HTTP/1.1\nAuthorization: Bearer abcdefghijkl\nbody contains " + SECRET);
    ReportAttachments.json(
        "response", "{\"user\":\"alice\",\"password\":\"hunter22\",\"echo\":\"" + SECRET + "\"}");
    ReportAttachments.json("not json", "token=" + SECRET + " {broken");
    ReportAttachments.text(null, null);
    ReportAttachments.png("screenshot " + SECRET, PNG);
    ReportAttachments.png("absent", null);
  }
}
