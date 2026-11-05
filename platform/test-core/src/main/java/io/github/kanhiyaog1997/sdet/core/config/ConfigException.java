package io.github.kanhiyaog1997.sdet.core.config;

/**
 * Signals invalid or missing test configuration.
 *
 * <p>Messages name the key and every source that was consulted, so a failing test run tells the
 * reader exactly what to set and where. Messages never contain secret values.
 */
public final class ConfigException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with a message.
   *
   * @param message what is wrong and how to fix it
   */
  public ConfigException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a message and the underlying cause.
   *
   * @param message what is wrong and how to fix it
   * @param cause the underlying failure
   */
  public ConfigException(String message, Throwable cause) {
    super(message, cause);
  }
}
