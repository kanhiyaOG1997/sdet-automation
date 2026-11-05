package io.github.kanhiyaog1997.sdet.reporting;

import io.qameta.allure.listener.ContainerLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.GlobalAttachment;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.Globals;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Masks sensitive data in Allure test results and containers immediately before Allure writes them
 * to the results directory.
 *
 * <p>Allure records failure messages, stack traces, parameter values, step names, links and
 * descriptions as they are. This listener runs in {@link #beforeTestWrite(TestResult)} and {@link
 * #beforeContainerWrite(TestResultContainer)}, the last callbacks before persistence, and passes
 * every one of those text fields, recursively through steps and fixtures, through {@link
 * SensitiveDataMasker}. Identifiers and relationships ({@code uuid}, {@code historyId}, {@code
 * testCaseId}, container children, attachment sources) are never changed.
 *
 * <p><b>Fails closed.</b> Allure catches and logs any exception a listener throws, then writes the
 * result anyway. If masking fails, this listener therefore replaces every maskable text field with
 * {@value #MASKING_FAILED} itself, so a masking problem loses information instead of leaking it.
 *
 * <p>Allure's run-level errors ({@code *-globals.json}) have no listener callback. They are masked
 * by {@link #maskGlobals(Globals)}, which {@link MaskingResultsWriter} calls immediately before
 * writing them, with the same rules and the same fail-closed behaviour.
 *
 * <p><b>Not covered:</b> the content of attachments written directly through Allure's API, which
 * Allure stores at attachment time (use {@link ReportAttachments} instead). Secrets must never be
 * passed as data-provider parameters: Allure derives {@code historyId} from parameter values before
 * this listener runs, and that identifier is deliberately left unchanged.
 *
 * <p>Registered through {@code META-INF/services/io.qameta.allure.listener.TestLifecycleListener}
 * and {@code META-INF/services/io.qameta.allure.listener.ContainerLifecycleListener}.
 */
public final class AllureResultMasker implements TestLifecycleListener, ContainerLifecycleListener {

  static final String MASKING_FAILED = "<masking failed>";

  private static final Logger LOG = LoggerFactory.getLogger(AllureResultMasker.class);

  private final Supplier<SensitiveDataMasker> masker;

  /** Creates the listener used by Allure, masking with the secrets of the current run. */
  public AllureResultMasker() {
    this(SensitiveDataMasker::forCurrentRun);
  }

  AllureResultMasker(Supplier<SensitiveDataMasker> masker) {
    this.masker = masker;
  }

  /** Masks a test result immediately before Allure writes it. */
  @Override
  public void beforeTestWrite(TestResult result) {
    if (result == null) {
      return;
    }
    try {
      maskTest(result, masker.get()::mask);
    } catch (RuntimeException e) {
      failClosed(e);
      maskTest(result, text -> text == null ? null : MASKING_FAILED);
    }
  }

  /** Masks a container, including its fixtures, immediately before Allure writes it. */
  @Override
  public void beforeContainerWrite(TestResultContainer container) {
    if (container == null) {
      return;
    }
    try {
      maskContainer(container, masker.get()::mask);
    } catch (RuntimeException e) {
      failClosed(e);
      maskContainer(container, text -> text == null ? null : MASKING_FAILED);
    }
  }

  /**
   * Masks Allure's run-level errors and the names of global attachments. Allure offers no listener
   * callback for these, so {@link MaskingResultsWriter} calls this immediately before writing them.
   * Timestamps, status flags and attachment sources, types and sizes are never changed.
   */
  void maskGlobals(Globals globals) {
    if (globals == null) {
      return;
    }
    try {
      maskGlobalsWith(globals, masker.get()::mask);
    } catch (RuntimeException e) {
      failClosed(e);
      maskGlobalsWith(globals, text -> text == null ? null : MASKING_FAILED);
    }
  }

  private static void maskGlobalsWith(Globals globals, UnaryOperator<String> mask) {
    if (globals.getErrors() != null) {
      for (GlobalError error : globals.getErrors()) {
        maskStatusDetails(error, mask);
      }
    }
    if (globals.getAttachments() != null) {
      for (GlobalAttachment attachment : globals.getAttachments()) {
        attachment.setName(mask.apply(attachment.getName()));
      }
    }
  }

  private static void maskTest(TestResult result, UnaryOperator<String> mask) {
    result.setName(mask.apply(result.getName()));
    result.setFullName(mask.apply(result.getFullName()));
    result.setTestCaseName(mask.apply(result.getTestCaseName()));
    if (result.getTitlePath() != null) {
      result.setTitlePath(result.getTitlePath().stream().map(mask).toList());
    }
    result.setDescription(mask.apply(result.getDescription()));
    result.setDescriptionHtml(mask.apply(result.getDescriptionHtml()));
    maskStatusDetails(result.getStatusDetails(), mask);
    maskParameters(result.getParameters(), mask);
    maskLinks(result.getLinks(), mask);
    maskLabels(result.getLabels(), mask);
    maskAttachments(result.getAttachments(), mask);
    maskSteps(result.getSteps(), mask);
  }

  private static void maskContainer(TestResultContainer container, UnaryOperator<String> mask) {
    container.setName(mask.apply(container.getName()));
    container.setDescription(mask.apply(container.getDescription()));
    container.setDescriptionHtml(mask.apply(container.getDescriptionHtml()));
    maskLinks(container.getLinks(), mask);
    maskFixtures(container.getBefores(), mask);
    maskFixtures(container.getAfters(), mask);
  }

  private static void maskFixtures(List<FixtureResult> fixtures, UnaryOperator<String> mask) {
    if (fixtures == null) {
      return;
    }
    for (FixtureResult fixture : fixtures) {
      fixture.setName(mask.apply(fixture.getName()));
      fixture.setDescription(mask.apply(fixture.getDescription()));
      fixture.setDescriptionHtml(mask.apply(fixture.getDescriptionHtml()));
      maskStatusDetails(fixture.getStatusDetails(), mask);
      maskParameters(fixture.getParameters(), mask);
      maskAttachments(fixture.getAttachments(), mask);
      maskSteps(fixture.getSteps(), mask);
    }
  }

  private static void maskSteps(List<StepResult> steps, UnaryOperator<String> mask) {
    if (steps == null) {
      return;
    }
    for (StepResult step : steps) {
      step.setName(mask.apply(step.getName()));
      step.setDescription(mask.apply(step.getDescription()));
      step.setDescriptionHtml(mask.apply(step.getDescriptionHtml()));
      maskStatusDetails(step.getStatusDetails(), mask);
      maskParameters(step.getParameters(), mask);
      maskAttachments(step.getAttachments(), mask);
      maskSteps(step.getSteps(), mask);
    }
  }

  private static void maskStatusDetails(StatusDetails details, UnaryOperator<String> mask) {
    if (details == null) {
      return;
    }
    details.setMessage(mask.apply(details.getMessage()));
    details.setTrace(mask.apply(details.getTrace()));
    details.setActual(mask.apply(details.getActual()));
    details.setExpected(mask.apply(details.getExpected()));
  }

  private static void maskParameters(List<Parameter> parameters, UnaryOperator<String> mask) {
    if (parameters == null) {
      return;
    }
    for (Parameter parameter : parameters) {
      String value = parameter.getValue();
      String masked = mask.apply(value);
      if (!Objects.equals(value, masked)) {
        parameter.setValue(masked);
        parameter.setMode(Parameter.Mode.MASKED);
      }
    }
  }

  private static void maskLinks(List<Link> links, UnaryOperator<String> mask) {
    if (links == null) {
      return;
    }
    for (Link link : links) {
      link.setName(mask.apply(link.getName()));
      link.setUrl(mask.apply(link.getUrl()));
    }
  }

  private static void maskLabels(List<Label> labels, UnaryOperator<String> mask) {
    if (labels == null) {
      return;
    }
    for (Label label : labels) {
      label.setValue(mask.apply(label.getValue()));
    }
  }

  private static void maskAttachments(List<Attachment> attachments, UnaryOperator<String> mask) {
    if (attachments == null) {
      return;
    }
    for (Attachment attachment : attachments) {
      attachment.setName(mask.apply(attachment.getName()));
    }
  }

  private static void failClosed(RuntimeException e) {
    LOG.warn(
        "Masking an Allure result failed ({}); its text fields are replaced with {}",
        e.getClass().getSimpleName(),
        MASKING_FAILED);
  }
}
