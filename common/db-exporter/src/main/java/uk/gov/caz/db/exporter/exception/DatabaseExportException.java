package uk.gov.caz.db.exporter.exception;

import uk.gov.caz.db.exporter.DatabaseExportDestination;

/**
 * Exception meaning failure during database export operation.
 */
public class DatabaseExportException extends RuntimeException {

  /**
   * Creates new instance of {@link DatabaseExportDestination} class.
   *
   * @param message Custom error message.
   */
  public DatabaseExportException(String message) {
    super(message);
  }

  /**
   * Creates new instance of {@link DatabaseExportDestination} class.
   *
   * @param message Custom error message.
   * @param cause Exception that was a root cause.
   */
  public DatabaseExportException(String message, Throwable cause) {
    super(message, cause);
  }
}
