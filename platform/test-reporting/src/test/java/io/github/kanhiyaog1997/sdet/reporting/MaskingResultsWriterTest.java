package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.qameta.allure.model.GlobalAttachment;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.Globals;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Masking of Allure's run-level errors by the results-writer wrapper, including failing closed. */
public class MaskingResultsWriterTest {

  private static final String SECRET = "TASK5-SECRET-12345";

  private RecordingResultsWriter recorded;
  private MaskingResultsWriter writer;

  /** TestNG reuses one instance for every method, so each method gets a fresh recorder. */
  @BeforeMethod
  public void newWriter() {
    recorded = new RecordingResultsWriter();
    writer =
        new MaskingResultsWriter(
            recorded, new AllureResultMasker(() -> new SensitiveDataMasker(List.of(SECRET))));
  }

  @Test
  public void everyTextFieldOfEveryGlobalErrorIsMaskedBeforeWriting() {
    Globals globals = globalsWithSecretEverywhere();

    writer.write(globals);

    assertThat(recorded.globals()).singleElement().asString().doesNotContain(SECRET);
    GlobalError first = globals.getErrors().get(0);
    assertThat(first.getMessage()).isEqualTo("setUp failed: " + MASK);
    assertThat(first.getTrace())
        .isEqualTo("java.lang.IllegalStateException: " + MASK + "\n\tat a.B.c(B.java:1)");
    assertThat(first.getActual()).isEqualTo("actual " + MASK);
    assertThat(first.getExpected()).isEqualTo("expected " + MASK);
    assertThat(globals.getErrors().get(1).getMessage()).isEqualTo("data provider: " + MASK);
    assertThat(globals.getAttachments().get(0).getName()).isEqualTo("log " + MASK);
  }

  @Test
  public void timestampsFlagsAndAttachmentMetadataAreNeverChanged() {
    Globals globals = globalsWithSecretEverywhere();

    writer.write(globals);

    GlobalError first = globals.getErrors().get(0);
    assertThat(first.getTimestamp()).isEqualTo(42L);
    assertThat(first.isKnown()).isTrue();
    assertThat(first.isMuted()).isTrue();
    assertThat(first.isFlaky()).isTrue();
    GlobalAttachment attachment = globals.getAttachments().get(0);
    assertThat(attachment.getTimestamp()).isEqualTo(7L);
    assertThat(attachment.getSource()).isEqualTo("abc-attachment.txt");
    assertThat(attachment.getType()).isEqualTo("text/plain");
    assertThat(attachment.getSize()).isEqualTo(12L);
  }

  @Test
  public void textWithoutSecretsIsWrittenUnchanged() {
    Globals globals =
        new Globals()
            .setErrors(new ArrayList<>(List.of(new GlobalError().setMessage("plain failure"))));

    writer.write(globals);

    assertThat(globals.getErrors().get(0).getMessage()).isEqualTo("plain failure");
  }

  @Test
  public void maskingFailuresFailClosedWithoutLoggingTheCause() {
    MaskingResultsWriter broken =
        new MaskingResultsWriter(
            recorded,
            new AllureResultMasker(
                () -> {
                  throw new IllegalStateException("masker broken while holding " + SECRET);
                }));
    Globals globals = globalsWithSecretEverywhere();

    try (LogCapture logs = new LogCapture(AllureResultMasker.class)) {
      broken.write(globals);

      assertThat(logs.messages()).singleElement().asString().contains("(IllegalStateException)");
      assertThat(logs.everything()).doesNotContain(SECRET);
    }
    assertThat(recorded.globals()).singleElement().asString().doesNotContain(SECRET);
    GlobalError first = globals.getErrors().get(0);
    assertThat(first.getMessage()).isEqualTo(AllureResultMasker.MASKING_FAILED);
    assertThat(first.getTrace()).isEqualTo(AllureResultMasker.MASKING_FAILED);
    assertThat(first.getTimestamp()).isEqualTo(42L);
    assertThat(globals.getAttachments().get(0).getName())
        .isEqualTo(AllureResultMasker.MASKING_FAILED);
    assertThat(globals.getAttachments().get(0).getSource()).isEqualTo("abc-attachment.txt");
  }

  @Test
  public void emptyGlobalsAreWrittenWithoutError() {
    Globals empty = new Globals().setErrors(null).setAttachments(null);

    assertThatCode(() -> writer.write(empty)).doesNotThrowAnyException();
    assertThat(recorded.globals()).hasSize(1);
  }

  @Test
  public void resultsContainersAndAttachmentsArePassedThroughUnchanged() {
    TestResult result = new TestResult().setUuid("u").setName("already masked by the listener");
    TestResultContainer container = new TestResultContainer().setUuid("c");
    byte[] content = "attachment".getBytes(StandardCharsets.UTF_8);

    writer.write(result);
    writer.write(container);
    writer.write("a-attachment.txt", new ByteArrayInputStream(content));

    assertThat(recorded.results())
        .singleElement()
        .asString()
        .contains("already masked by the listener");
    assertThat(recorded.containers()).singleElement().asString().contains("\"uuid\":\"c\"");
    assertThat(recorded.attachments()).containsOnlyKeys("a-attachment.txt");
    assertThat(recorded.attachments().get("a-attachment.txt")).isEqualTo(content);
  }

  private static Globals globalsWithSecretEverywhere() {
    GlobalError configuration =
        new GlobalError()
            .setTimestamp(42L)
            .setKnown(true)
            .setMuted(true)
            .setFlaky(true)
            .setMessage("setUp failed: " + SECRET)
            .setTrace("java.lang.IllegalStateException: " + SECRET + "\n\tat a.B.c(B.java:1)")
            .setActual("actual " + SECRET)
            .setExpected("expected " + SECRET);
    GlobalError dataProvider = new GlobalError().setMessage("data provider: " + SECRET);
    GlobalAttachment attachment =
        new GlobalAttachment()
            .setTimestamp(7L)
            .setName("log " + SECRET)
            .setSource("abc-attachment.txt")
            .setType("text/plain")
            .setSize(12L);
    return new Globals()
        .setErrors(new ArrayList<>(List.of(configuration, dataProvider)))
        .setAttachments(new ArrayList<>(List.of(attachment)));
  }
}
