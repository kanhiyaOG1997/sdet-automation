package io.github.kanhiyaog1997.sdet.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.Globals;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An Allure results writer that serialises every object at the moment Allure persists it, so tests
 * can assert on exactly what would have been written to disk.
 */
final class RecordingResultsWriter implements AllureResultsWriter {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final List<String> results = new CopyOnWriteArrayList<>();
  private final List<String> containers = new CopyOnWriteArrayList<>();
  private final List<String> globals = new CopyOnWriteArrayList<>();
  private final Map<String, byte[]> attachments = new ConcurrentHashMap<>();

  @Override
  public void write(TestResult result) {
    results.add(serialise(result));
  }

  @Override
  public void write(TestResultContainer container) {
    containers.add(serialise(container));
  }

  @Override
  public void write(Globals value) {
    globals.add(serialise(value));
  }

  @Override
  public void write(String source, InputStream attachment) {
    try (InputStream in = attachment) {
      attachments.put(source, in.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  List<String> results() {
    return List.copyOf(results);
  }

  List<String> containers() {
    return List.copyOf(containers);
  }

  List<String> globals() {
    return List.copyOf(globals);
  }

  Map<String, byte[]> attachments() {
    return Map.copyOf(attachments);
  }

  private static String serialise(Object value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not serialise " + value.getClass().getSimpleName(), e);
    }
  }
}
