package uk.gov.caz.db.exporter;

import java.io.OutputStream;
import java.net.URI;

/**
 * Specifies operations required from implementers of destination targets for database export
 * stream. Implementers should provide new instance of this interface for each export operation.
 */
public interface DatabaseExportDestination {

  /**
   * Destination should provide {@link OutputStream} into which database export will stream its
   * data. This operation should be the first one called on {@link DatabaseExportDestination}
   * implementation.
   *
   * @return Destination {@link OutputStream} which will get export data stream.
   */
  OutputStream outputStream();

  /**
   * Lets the implementation know that it is time to flush all data already streamed into {@link
   * OutputStream}.
   */
  void flush();

  /**
   * Lets the implementation know that all data have been streamed and it can now close and finalize
   * all connections, buffers etc.
   */
  void close();

  /**
   * Destination must provide full {@link URI} to stored export data. This operation may throw an
   * exception if destination is not yet ready because it was not filled with data and
   * flushed/closed.
   *
   * @return Full link to stored export data.
   */
  URI getDestinationUri();
}
