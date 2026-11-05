package io.github.kanhiyaog1997.sdet.reporting;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.Globals;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.io.InputStream;
import java.util.Objects;

/**
 * Wraps Allure's results writer so that run-level errors ({@code *-globals.json}) are masked
 * immediately before they are written.
 *
 * <p>Allure writes these errors through {@code AllureLifecycle.writeGlobals}, which calls the
 * writer directly without notifying any listener. {@code AllureTestNg} does this when a
 * configuration method or a data provider fails, copying the exception message and stack trace. The
 * writer is therefore the last point where they can be masked before persistence.
 *
 * <p>Test results and containers are passed through unchanged: {@link AllureResultMasker} has
 * already masked them in Allure's {@code beforeTestWrite} and {@code beforeContainerWrite}
 * callbacks. Attachment content is passed through unchanged, as Allure provides it (see the module
 * README).
 */
final class MaskingResultsWriter implements AllureResultsWriter {

  private final AllureResultsWriter delegate;
  private final AllureResultMasker masker;

  MaskingResultsWriter(AllureResultsWriter delegate, AllureResultMasker masker) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.masker = Objects.requireNonNull(masker, "masker");
  }

  @Override
  public void write(TestResult result) {
    delegate.write(result);
  }

  @Override
  public void write(TestResultContainer container) {
    delegate.write(container);
  }

  /** Masks the run-level errors, failing closed, then writes them. */
  @Override
  public void write(Globals globals) {
    masker.maskGlobals(globals);
    delegate.write(globals);
  }

  @Override
  public void write(String source, InputStream attachment) {
    delegate.write(source, attachment);
  }
}
