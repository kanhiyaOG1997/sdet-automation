package io.github.kanhiyaog1997.sdet.reporting;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Captures the log events of one logger, so tests can assert on exactly what was logged. */
final class LogCapture implements AutoCloseable {

  private final Logger logger;
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  LogCapture(Class<?> type) {
    this.logger = (Logger) LoggerFactory.getLogger(type);
    appender.start();
    logger.addAppender(appender);
  }

  List<ILoggingEvent> events() {
    return List.copyOf(appender.list);
  }

  List<String> messages() {
    return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }

  /** Everything an appender could print: messages, MDC values and attached throwables. */
  String everything() {
    List<String> parts = new ArrayList<>();
    for (ILoggingEvent event : appender.list) {
      parts.add(event.getFormattedMessage());
      parts.add(String.valueOf(event.getMDCPropertyMap()));
      for (IThrowableProxy t = event.getThrowableProxy(); t != null; t = t.getCause()) {
        parts.add(t.getClassName() + ": " + t.getMessage());
      }
    }
    return String.join("\n", parts);
  }

  @Override
  public void close() {
    logger.detachAppender(appender);
    appender.stop();
  }
}
