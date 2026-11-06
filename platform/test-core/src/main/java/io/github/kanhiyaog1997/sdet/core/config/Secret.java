package io.github.kanhiyaog1997.sdet.core.config;

import java.util.Objects;

/**
 * A sensitive configuration value, such as a password or token.
 *
 * <p>{@link #toString()} always returns a mask, so a secret printed by accident in a log line,
 * assertion message or report shows {@code ****} instead of its value. The value is available only
 * through an explicit call to {@link #reveal()}.
 */
public final class Secret {

  private static final String MASK = "****";

  private final String value;

  Secret(String value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  /**
   * Returns the raw secret value. Call this only where the value is actually needed.
   *
   * @return the unmasked value
   */
  public String reveal() {
    return value;
  }

  /**
   * Returns a fixed mask, never the value.
   *
   * @return {@code ****}
   */
  @Override
  public String toString() {
    return MASK;
  }
}
