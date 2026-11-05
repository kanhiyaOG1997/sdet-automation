package io.github.kanhiyaog1997.sdet.reporting;

import io.github.kanhiyaog1997.sdet.core.config.ConfigException;
import io.github.kanhiyaog1997.sdet.core.config.TestConfig;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Masks sensitive data in text before it reaches logs, report attachments or environment
 * information.
 *
 * <p>Two layers are applied, in this order:
 *
 * <ol>
 *   <li><b>Value-based:</b> every occurrence of a configured secret value that has at least {@value
 *       #MINIMUM_VALUE_LENGTH} characters is replaced with {@value #MASK}. Matching is exact and
 *       literal, so regex-special and Unicode characters are safe. Overlapping occurrences are
 *       merged, so no fragment of a secret survives.
 *   <li><b>Key-based:</b> values that follow a sensitive key name ({@code authorization}, {@code
 *       cookie}, {@code password}, {@code passwd}, {@code token}, {@code secret}, {@code api-key},
 *       {@code credential}) are masked in header lines, JSON members and {@code key=value} text,
 *       and {@code Bearer} tokens are masked anywhere.
 * </ol>
 *
 * <p><b>Limitations.</b> Secrets shorter than {@value #MINIMUM_VALUE_LENGTH} characters are not
 * masked by value, because masking them would corrupt ordinary text. Encoded forms of a secret (URL
 * encoding, Base64, hashes) and secrets that are not registered through the {@code secret.}
 * namespace are only caught by the key-based rules, if at all. Masking reduces the risk of leaks;
 * it cannot guarantee that no secret ever appears.
 */
public final class SensitiveDataMasker {

  /** The replacement written in place of sensitive data. */
  public static final String MASK = "****";

  /** Secrets shorter than this are not masked by value (design input S4). */
  public static final int MINIMUM_VALUE_LENGTH = 4;

  private static final Logger LOG = LoggerFactory.getLogger(SensitiveDataMasker.class);
  private static final AtomicBoolean CONFIG_WARNING_LOGGED = new AtomicBoolean();

  private static final String SENSITIVE_KEY =
      "(?:authorization|cookie|password|passwd|token|secret|api[-_]?key|credential)";

  /** {@code Authorization: Basic abc}, {@code Set-Cookie: id=1}, {@code password: x}. */
  private static final Pattern HEADER_LINE =
      Pattern.compile(
          "(?im)^([ \\t]*[a-z0-9_-]*" + SENSITIVE_KEY + "[a-z0-9_-]*[ \\t]*:[ \\t]*)[^\\r\\n]+");

  /** {@code "password": "x"}, {@code "access_token": 123}. */
  private static final Pattern JSON_MEMBER =
      Pattern.compile(
          "(?i)(\"[^\"\\r\\n]*"
              + SENSITIVE_KEY
              + "[^\"\\r\\n]*\"\\s*:\\s*)(?:\"(?:[^\"\\\\]|\\\\.)*\"|-?\\d[\\d.eE+-]*|true|false)");

  /** {@code password=x}, {@code ?api_key=x&page=2}, {@code token=x;}. */
  private static final Pattern KEY_VALUE =
      Pattern.compile(
          "(?i)((?:^|[?&;,\\s])[^=&?;,\\s\"']*" + SENSITIVE_KEY + "[^=&?;,\\s\"']*=)[^&;,\\s\"']+");

  /** {@code Bearer eyJhbGciOi...} anywhere in the text. */
  private static final Pattern BEARER_TOKEN =
      Pattern.compile("(?i)\\b(Bearer\\s+)[A-Za-z0-9._~+/=-]{8,}");

  private final List<String> secrets;
  private final int ignoredShortSecrets;

  /**
   * Creates a masker for the given secret values. Values shorter than {@value
   * #MINIMUM_VALUE_LENGTH} characters are ignored for value-based masking; their number, never
   * their content, is logged.
   *
   * @param secretValues the secret values to mask, for example {@link TestConfig#secretValues()}
   */
  public SensitiveDataMasker(Collection<String> secretValues) {
    Objects.requireNonNull(secretValues, "secretValues");
    Set<String> eligible = new TreeSet<>();
    int ignored = 0;
    for (String value : secretValues) {
      if (value == null || value.isEmpty()) {
        continue;
      }
      if (value.length() >= MINIMUM_VALUE_LENGTH) {
        eligible.add(value);
      } else {
        ignored++;
      }
    }
    this.secrets = List.copyOf(eligible);
    this.ignoredShortSecrets = ignored;
    if (ignored > 0) {
      LOG.warn(
          "{} configured secret value(s) are shorter than {} characters and are not masked by"
              + " value; key-based masking still applies",
          ignored,
          MINIMUM_VALUE_LENGTH);
    }
  }

  /**
   * Creates a masker for the secrets of the current test run, from {@link TestConfig#current()}. If
   * the configuration cannot be loaded, the masker falls back to key-based masking only and logs
   * one warning that names the exception type, never its message.
   *
   * @return a masker for this run
   */
  public static SensitiveDataMasker forCurrentRun() {
    try {
      return new SensitiveDataMasker(TestConfig.current().secretValues());
    } catch (ConfigException e) {
      if (CONFIG_WARNING_LOGGED.compareAndSet(false, true)) {
        LOG.warn(
            "Test configuration could not be loaded ({}); masking falls back to key-based rules"
                + " only",
            e.getClass().getSimpleName());
      }
      return new SensitiveDataMasker(List.of());
    }
  }

  /**
   * Returns {@code text} with all sensitive data replaced by {@value #MASK}.
   *
   * @param text the text to mask; may be {@code null}
   * @return the masked text, or {@code null} if {@code text} was {@code null}
   */
  public String mask(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String masked = maskValues(text);
    masked = JSON_MEMBER.matcher(masked).replaceAll("$1\"" + MASK + "\"");
    masked = HEADER_LINE.matcher(masked).replaceAll("$1" + MASK);
    masked = KEY_VALUE.matcher(masked).replaceAll("$1" + MASK);
    return BEARER_TOKEN.matcher(masked).replaceAll("$1" + MASK);
  }

  int ignoredShortSecrets() {
    return ignoredShortSecrets;
  }

  private String maskValues(String text) {
    if (secrets.isEmpty()) {
      return text;
    }
    boolean[] sensitive = new boolean[text.length()];
    boolean found = false;
    for (String secret : secrets) {
      int from = 0;
      int index;
      while ((index = text.indexOf(secret, from)) >= 0) {
        for (int i = index; i < index + secret.length(); i++) {
          sensitive[i] = true;
        }
        found = true;
        from = index + 1;
      }
    }
    if (!found) {
      return text;
    }
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      if (!sensitive[i]) {
        out.append(text.charAt(i));
      } else if (i == 0 || !sensitive[i - 1]) {
        out.append(MASK);
      }
    }
    return out.toString();
  }
}
