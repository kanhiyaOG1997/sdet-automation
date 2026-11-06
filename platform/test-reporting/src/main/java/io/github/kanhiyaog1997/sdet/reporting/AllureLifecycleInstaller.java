package io.github.kanhiyaog1997.sdet.reporting;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.testng.AllureTestNg;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.IExecutionListener;
import org.testng.ITestNGListener;

/**
 * Makes Allure write through {@link MaskingResultsWriter}, so that run-level errors are masked
 * before they reach the results directory.
 *
 * <p>Allure 3.0.0 has no extension point for its results writer: the default lifecycle always
 * writes through a plain {@link FileSystemResultsWriter}. The supported way to change the writer is
 * to give Allure a lifecycle built with one, through {@link Allure#setLifecycle(AllureLifecycle)}.
 * {@link AllureTestNg} keeps the lifecycle that is current when it is constructed, so this must
 * happen <em>before</em> TestNG constructs it.
 *
 * <p>TestNG constructs ServiceLoader listeners in the order their {@code
 * META-INF/services/org.testng.ITestNGListener} entries appear on the class path. This class is the
 * first entry in this module's file, and this module comes before {@code allure-testng} on the
 * class path because {@code allure-testng} is one of its dependencies. The installation runs in the
 * constructor and checks that order first: if {@code AllureTestNg} would be constructed first, it
 * throws, so the TestNG run fails instead of writing unmasked errors.
 *
 * <p>The lifecycle is installed once per JVM; later instances, for example from nested TestNG runs,
 * change nothing.
 */
public final class AllureLifecycleInstaller implements IExecutionListener {

  static final String SERVICE_FILE = "META-INF/services/" + ITestNGListener.class.getName();

  private static final Object LOCK = new Object();

  private static volatile AllureLifecycle installed;

  /**
   * Installs the masking lifecycle, unless it is already installed.
   *
   * @throws IllegalStateException if {@code AllureTestNg} is registered before this class
   */
  public AllureLifecycleInstaller() {
    install();
  }

  /** Returns the lifecycle installed in this JVM, installing it first if necessary. */
  static AllureLifecycle install() {
    synchronized (LOCK) {
      if (installed != null) {
        return installed;
      }
      requireRegisteredBeforeAllureTestNg(registeredListeners(contextClassLoader()));
      AllureLifecycle lifecycle =
          new AllureLifecycle(
              new MaskingResultsWriter(
                  new FileSystemResultsWriter(
                      AllureEnvironmentListener.configuredResultsDirectory()),
                  new AllureResultMasker()));
      Allure.setLifecycle(lifecycle);
      installed = lifecycle;
      return lifecycle;
    }
  }

  /** Returns the lifecycle installed in this JVM, or {@code null} if none is installed yet. */
  static AllureLifecycle installed() {
    return installed;
  }

  /**
   * Fails unless this class is registered before {@link AllureTestNg}.
   *
   * @param listeners the class names of the registered TestNG listeners, in registration order
   * @throws IllegalStateException if {@code AllureTestNg} is registered but this class is not, or
   *     is registered after it
   */
  static void requireRegisteredBeforeAllureTestNg(List<String> listeners) {
    int allure = listeners.indexOf(AllureTestNg.class.getName());
    int self = listeners.indexOf(AllureLifecycleInstaller.class.getName());
    if (allure >= 0 && (self < 0 || allure < self)) {
      throw new IllegalStateException(
          AllureTestNg.class.getName()
              + " is registered before "
              + AllureLifecycleInstaller.class.getName()
              + ", so Allure would write run-level errors without masking them. Put the"
              + " test-reporting dependency before any direct allure-testng dependency (better:"
              + " remove the direct dependency; test-reporting already provides it).");
    }
  }

  /** Reads every TestNG ServiceLoader registration visible to the loader, in class-path order. */
  static List<String> registeredListeners(ClassLoader loader) {
    List<String> names = new ArrayList<>();
    try {
      for (URL file : Collections.list(loader.getResources(SERVICE_FILE))) {
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(file.openStream(), StandardCharsets.UTF_8))) {
          reader.lines().map(AllureLifecycleInstaller::providerName).forEach(names::add);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + SERVICE_FILE, e);
    }
    names.removeIf(String::isEmpty);
    return names;
  }

  private static String providerName(String line) {
    int comment = line.indexOf('#');
    return (comment >= 0 ? line.substring(0, comment) : line).trim();
  }

  /** The loader {@code ServiceLoader.load(Class)} uses, which is what TestNG calls. */
  private static ClassLoader contextClassLoader() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return loader != null ? loader : ClassLoader.getSystemClassLoader();
  }
}
