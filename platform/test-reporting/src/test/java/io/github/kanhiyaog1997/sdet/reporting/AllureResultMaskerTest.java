package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/** Field-by-field behaviour of the Allure result masker, including failing closed. */
public class AllureResultMaskerTest {

  private static final String SECRET = "TASK5-SECRET-12345";
  private static final ObjectMapper JSON = new ObjectMapper();

  private final AllureResultMasker masker =
      new AllureResultMasker(() -> new SensitiveDataMasker(List.of(SECRET)));

  @Test
  public void everyTextFieldOfTestResultsIsMaskedRecursively() throws Exception {
    TestResult result = resultWithSecretEverywhere();

    masker.beforeTestWrite(result);

    assertThat(JSON.writeValueAsString(result)).doesNotContain(SECRET);
    assertThat(result.getName()).isEqualTo("name " + MASK);
    assertThat(result.getFullName()).isEqualTo("full " + MASK);
    assertThat(result.getTestCaseName()).isEqualTo("case " + MASK);
    assertThat(result.getTitlePath()).containsExactly("suite", "path " + MASK);
    assertThat(result.getDescription()).isEqualTo("description " + MASK);
    assertThat(result.getDescriptionHtml()).isEqualTo("<p>" + MASK + "</p>");
    assertThat(result.getStatusDetails().getMessage()).isEqualTo("message " + MASK);
    assertThat(result.getStatusDetails().getTrace()).contains("Caused by: cause " + MASK);
    assertThat(result.getStatusDetails().getActual()).isEqualTo("actual " + MASK);
    assertThat(result.getStatusDetails().getExpected()).isEqualTo("expected " + MASK);
    assertThat(result.getLinks().get(0).getUrl()).isEqualTo("https://x.test/?token=" + MASK);
    assertThat(result.getLabels().get(0).getValue()).isEqualTo("label " + MASK);
    assertThat(result.getAttachments().get(0).getName()).isEqualTo("attachment " + MASK);
    StepResult deepest = result.getSteps().get(0).getSteps().get(0).getSteps().get(0);
    assertThat(deepest.getName()).isEqualTo("level 3 " + MASK);
    assertThat(deepest.getStatusDetails().getMessage()).isEqualTo("step failure " + MASK);
    assertThat(deepest.getParameters().get(0).getValue()).isEqualTo(MASK);
    assertThat(deepest.getAttachments().get(0).getName()).isEqualTo("step attachment " + MASK);
  }

  @Test
  public void identifiersRelationshipsAndHistoryIdAreNeverChanged() {
    TestResult result = resultWithSecretEverywhere();

    masker.beforeTestWrite(result);

    assertThat(result.getUuid()).isEqualTo("uuid-1");
    assertThat(result.getHistoryId()).isEqualTo("history-1");
    assertThat(result.getTestCaseId()).isEqualTo("case-id-1");
    assertThat(result.getStatus()).isEqualTo(Status.FAILED);
    assertThat(result.getStart()).isEqualTo(1L);
    assertThat(result.getStop()).isEqualTo(2L);
    assertThat(result.getLabels().get(0).getName()).isEqualTo("tag");
    assertThat(result.getLinks().get(0).getType()).isEqualTo("issue");
    Attachment attachment = result.getAttachments().get(0);
    assertThat(attachment.getSource()).isEqualTo("abc-attachment.txt");
    assertThat(attachment.getType()).isEqualTo("text/plain");
    assertThat(result.getParameters().get(0).getName()).isEqualTo("credential");
  }

  @Test
  public void maskedParametersAreFlaggedAndUntouchedParametersKeepTheirMode() {
    TestResult result = resultWithSecretEverywhere();

    masker.beforeTestWrite(result);

    Parameter secret = result.getParameters().get(0);
    Parameter plain = result.getParameters().get(1);
    assertThat(secret.getValue()).isEqualTo(MASK);
    assertThat(secret.getMode()).isEqualTo(Parameter.Mode.MASKED);
    assertThat(plain.getValue()).isEqualTo("chrome");
    assertThat(plain.getMode()).isNull();
  }

  @Test
  public void containersAndTheirFixturesAreMasked() throws Exception {
    TestResultContainer container =
        new TestResultContainer()
            .setUuid("container-1")
            .setName("container " + SECRET)
            .setDescription("container description " + SECRET)
            .setChildren(new ArrayList<>(List.of("uuid-1")))
            .setLinks(new ArrayList<>(List.of(new Link().setName("l").setUrl("u?" + SECRET))))
            .setBefores(new ArrayList<>(List.of(fixture("setUp " + SECRET))))
            .setAfters(new ArrayList<>(List.of(fixture("tearDown " + SECRET))));

    masker.beforeContainerWrite(container);

    assertThat(JSON.writeValueAsString(container)).doesNotContain(SECRET);
    assertThat(container.getUuid()).isEqualTo("container-1");
    assertThat(container.getChildren()).containsExactly("uuid-1");
    FixtureResult setUp = container.getBefores().get(0);
    assertThat(setUp.getName()).isEqualTo("setUp " + MASK);
    assertThat(setUp.getStatusDetails().getMessage()).isEqualTo("fixture failed " + MASK);
    assertThat(setUp.getSteps().get(0).getName()).isEqualTo("fixture step " + MASK);
  }

  @Test
  public void maskingFailuresFailClosedWithoutLoggingTheCause() throws Exception {
    AllureResultMasker broken =
        new AllureResultMasker(
            () -> {
              throw new IllegalStateException("masker broken while holding " + SECRET);
            });
    TestResult result = resultWithSecretEverywhere();
    TestResultContainer container =
        new TestResultContainer()
            .setUuid("container-1")
            .setName("container " + SECRET)
            .setBefores(new ArrayList<>(List.of(fixture("setUp " + SECRET))));

    try (LogCapture logs = new LogCapture(AllureResultMasker.class)) {
      broken.beforeTestWrite(result);
      broken.beforeContainerWrite(container);

      assertThat(logs.messages())
          .hasSize(2)
          .allSatisfy(m -> assertThat(m).contains("(IllegalStateException)"));
      assertThat(logs.everything()).doesNotContain(SECRET);
    }
    assertThat(JSON.writeValueAsString(result)).doesNotContain(SECRET);
    assertThat(JSON.writeValueAsString(container)).doesNotContain(SECRET);
    assertThat(result.getName()).isEqualTo(AllureResultMasker.MASKING_FAILED);
    assertThat(result.getStatusDetails().getTrace()).isEqualTo(AllureResultMasker.MASKING_FAILED);
    assertThat(container.getBefores().get(0).getName())
        .isEqualTo(AllureResultMasker.MASKING_FAILED);
    assertThat(result.getUuid()).isEqualTo("uuid-1");
    assertThat(container.getUuid()).isEqualTo("container-1");
  }

  @Test
  public void nullObjectsAndMissingFieldsAreHandled() {
    TestResult sparse =
        new TestResult()
            .setUuid("u")
            .setTitlePath(null)
            .setSteps(null)
            .setLinks(null)
            .setLabels(null)
            .setParameters(null)
            .setAttachments(null);
    TestResultContainer empty = new TestResultContainer().setBefores(null).setAfters(null);

    assertThatCode(
            () -> {
              masker.beforeTestWrite(null);
              masker.beforeContainerWrite(null);
              masker.beforeTestWrite(sparse);
              masker.beforeContainerWrite(empty);
            })
        .doesNotThrowAnyException();
    assertThat(sparse.getName()).isNull();
    assertThat(sparse.getUuid()).isEqualTo("u");
  }

  // ---- fixtures ----

  private static TestResult resultWithSecretEverywhere() {
    StepResult level3 =
        new StepResult()
            .setName("level 3 " + SECRET)
            .setStatusDetails(new StatusDetails().setMessage("step failure " + SECRET))
            .setParameters(new ArrayList<>(List.of(new Parameter().setName("p").setValue(SECRET))))
            .setAttachments(
                new ArrayList<>(
                    List.of(new Attachment().setName("step attachment " + SECRET).setSource("s"))));
    StepResult level2 =
        new StepResult().setName("level 2").setSteps(new ArrayList<>(List.of(level3)));
    StepResult level1 =
        new StepResult().setName("level 1 " + SECRET).setSteps(new ArrayList<>(List.of(level2)));
    return new TestResult()
        .setUuid("uuid-1")
        .setHistoryId("history-1")
        .setTestCaseId("case-id-1")
        .setName("name " + SECRET)
        .setFullName("full " + SECRET)
        .setTestCaseName("case " + SECRET)
        .setTitlePath(new ArrayList<>(List.of("suite", "path " + SECRET)))
        .setDescription("description " + SECRET)
        .setDescriptionHtml("<p>" + SECRET + "</p>")
        .setStatus(Status.FAILED)
        .setStart(1L)
        .setStop(2L)
        .setStatusDetails(
            new StatusDetails()
                .setMessage("message " + SECRET)
                .setTrace(
                    "java.lang.AssertionError: message "
                        + SECRET
                        + "\n\tat a.B.c(B.java:1)\nCaused by: cause "
                        + SECRET)
                .setActual("actual " + SECRET)
                .setExpected("expected " + SECRET))
        .setParameters(
            new ArrayList<>(
                List.of(
                    new Parameter().setName("credential").setValue(SECRET),
                    new Parameter().setName("browser").setValue("chrome"))))
        .setLinks(
            new ArrayList<>(
                List.of(
                    new Link()
                        .setName("issue " + SECRET)
                        .setUrl("https://x.test/?token=" + SECRET)
                        .setType("issue"))))
        .setLabels(new ArrayList<>(List.of(new Label().setName("tag").setValue("label " + SECRET))))
        .setAttachments(
            new ArrayList<>(
                List.of(
                    new Attachment()
                        .setName("attachment " + SECRET)
                        .setSource("abc-attachment.txt")
                        .setType("text/plain"))))
        .setSteps(new ArrayList<>(List.of(level1)));
  }

  private static FixtureResult fixture(String name) {
    return new FixtureResult()
        .setName(name)
        .setStatusDetails(new StatusDetails().setMessage("fixture failed " + SECRET))
        .setSteps(new ArrayList<>(List.of(new StepResult().setName("fixture step " + SECRET))));
  }
}
