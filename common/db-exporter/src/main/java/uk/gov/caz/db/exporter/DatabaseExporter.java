package uk.gov.caz.db.exporter;

import java.net.URI;

/**
 * Represents database export operation into {@link DatabaseExportDestination}.
 */
public interface DatabaseExporter {

  /**
   * Exports database data into {@link DatabaseExportDestination} implementation. Important note
   * here is that implementation must close destination so caller should be aware of this fact.
   *
   * @param destination {@link DatabaseExportDestination} into which data will be exported.
   * @return Full link to stored export data.
   */
  URI exportTo(DatabaseExportDestination destination);
}
