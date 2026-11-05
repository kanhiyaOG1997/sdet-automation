package io.github.kanhiyaog1997.sdet.reporting;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;

/**
 * Adds attachments to the Allure report for the currently running test.
 *
 * <p>Every text attachment, and every attachment name, passes through {@link SensitiveDataMasker}
 * before it is handed to Allure, so masked content is what gets written to the results directory.
 * There is deliberately no method that attaches text without masking.
 *
 * <p>Images cannot be masked: a screenshot that shows a secret on screen will contain it. Called
 * outside a running test or fixture, Allure ignores the attachment and logs a warning.
 */
public final class ReportAttachments {

  static final String DEFAULT_NAME = "attachment";

  private static final String TEXT_PLAIN = "text/plain";
  private static final String APPLICATION_JSON = "application/json";
  private static final String IMAGE_PNG = "image/png";

  private static final ObjectMapper JSON =
      JsonMapper.builder(
              JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .build();

  private ReportAttachments() {}

  /**
   * Attaches masked plain text.
   *
   * @param name the attachment name shown in the report; masked; {@code null} or blank becomes
   *     {@value #DEFAULT_NAME}
   * @param content the text; masked; {@code null} is attached as empty text
   */
  public static void text(String name, String content) {
    SensitiveDataMasker masker = SensitiveDataMasker.forCurrentRun();
    attachText(masker, name, TEXT_PLAIN, ".txt", content);
  }

  /**
   * Attaches masked JSON. Valid JSON is pretty-printed first and masked afterwards, so the masked
   * form is exactly what is attached. Content that is not a single valid JSON document is attached
   * unchanged as masked plain text.
   *
   * @param name the attachment name shown in the report; masked; {@code null} or blank becomes
   *     {@value #DEFAULT_NAME}
   * @param json the JSON document; {@code null} is attached as empty text
   */
  public static void json(String name, String json) {
    SensitiveDataMasker masker = SensitiveDataMasker.forCurrentRun();
    String pretty = prettyJson(json);
    if (pretty == null) {
      attachText(masker, name, TEXT_PLAIN, ".txt", json);
    } else {
      attachText(masker, name, APPLICATION_JSON, ".json", pretty);
    }
  }

  /**
   * Attaches a PNG image. The image bytes cannot be masked; only the name is.
   *
   * @param name the attachment name shown in the report; masked; {@code null} or blank becomes
   *     {@value #DEFAULT_NAME}
   * @param png the image bytes; {@code null} attaches nothing
   */
  public static void png(String name, byte[] png) {
    if (png == null) {
      return;
    }
    Allure.attachment(
        safeName(SensitiveDataMasker.forCurrentRun(), name),
        IMAGE_PNG,
        new ByteArrayInputStream(png.clone()),
        AttachmentOptions.withFileExtension(".png"));
  }

  /**
   * Returns {@code json} pretty-printed, or {@code null} if it is not a single valid JSON document.
   * Parse errors are never logged, because parser messages can quote the input.
   */
  static String prettyJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      JsonNode node = JSON.readTree(json);
      return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  static String safeName(SensitiveDataMasker masker, String name) {
    if (name == null || name.isBlank()) {
      return DEFAULT_NAME;
    }
    return masker.mask(name);
  }

  private static void attachText(
      SensitiveDataMasker masker, String name, String type, String extension, String content) {
    String masked = content == null ? "" : masker.mask(content);
    Allure.attachment(
        safeName(masker, name), type, masked, AttachmentOptions.withFileExtension(extension));
  }
}
