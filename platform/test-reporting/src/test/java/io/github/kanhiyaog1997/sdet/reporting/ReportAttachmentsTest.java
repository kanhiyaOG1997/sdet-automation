package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.testng.annotations.Test;

/**
 * JSON handling, attachment names and behaviour outside a running test. Masking of the attachment
 * content itself is proven end to end in {@link ReportingWiringTest}.
 */
public class ReportAttachmentsTest {

  private static final String SECRET = "TASK5-SECRET-12345";

  @Test
  public void validJsonIsPrettyPrinted() {
    assertThat(ReportAttachments.prettyJson("{\"a\":1,\"b\":[true,null]}"))
        .contains("\"a\" : 1")
        .contains("\n");
  }

  @Test
  public void invalidJsonIsNotPrettyPrintedSoNothingIsSilentlyDropped() {
    assertThat(ReportAttachments.prettyJson("{\"a\": ")).isNull();
    assertThat(ReportAttachments.prettyJson("{\"a\":1} trailing")).isNull();
    assertThat(ReportAttachments.prettyJson("{\"a\":1,\"a\":2}")).isNull();
    assertThat(ReportAttachments.prettyJson(null)).isNull();
    assertThat(ReportAttachments.prettyJson("  ")).isNull();
  }

  @Test
  public void prettyPrintingPreservesDecimalPrecision() {
    assertThat(ReportAttachments.prettyJson("{\"v\":0.10000000000000000555}"))
        .contains("0.10000000000000000555");
  }

  @Test
  public void attachmentNamesAreMaskedAndDefaulted() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET));

    assertThat(ReportAttachments.safeName(masker, "request " + SECRET))
        .isEqualTo("request " + MASK);
    assertThat(ReportAttachments.safeName(masker, null)).isEqualTo(ReportAttachments.DEFAULT_NAME);
    assertThat(ReportAttachments.safeName(masker, " ")).isEqualTo(ReportAttachments.DEFAULT_NAME);
  }

  @Test
  public void attachingOutsideRunningTestsIsIgnoredWithoutError() {
    assertThatCode(
            () -> {
              ReportAttachments.text("outside", SECRET);
              ReportAttachments.json("outside", "{}");
              ReportAttachments.png("outside", new byte[] {1});
              ReportAttachments.png("absent", null);
            })
        .doesNotThrowAnyException();
  }
}
