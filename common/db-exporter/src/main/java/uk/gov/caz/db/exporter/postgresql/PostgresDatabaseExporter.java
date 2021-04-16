package uk.gov.caz.db.exporter.postgresql;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.Setter;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import uk.gov.caz.db.exporter.DatabaseExportDestination;
import uk.gov.caz.db.exporter.DatabaseExporter;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

/**
 * Database exporter for Postgres using Postgres JDBC {@link CopyManager} helper.
 */
public class PostgresDatabaseExporter implements DatabaseExporter {

  /**
   * {@link DataSource} that allows to get a connection to the DB.
   */
  private final DataSource dataSource;

  /**
   * Client's specified query that will be fed to {@link CopyManager} to export the data.
   */
  private final String postgresExportQuery;

  /**
   * Supplies {@link CopyManager}, by default real Postgres implementation. Usable in tests to
   * provide own/mocked implementation.
   */
  @Setter(AccessLevel.PACKAGE)
  private Function<Connection, CopyManager> copyManagerSupplier;

  /**
   * Creates new instance of {@link PostgresDatabaseExporter} class.
   *
   * @param dataSource {@link DataSource} that allows to get a connection to the DB.
   * @param postgresExportQuery Client's specified query that will be fed to {@link CopyManager}
   *     to export the data.
   */
  public PostgresDatabaseExporter(DataSource dataSource, String postgresExportQuery) {
    this.dataSource = dataSource;
    this.postgresExportQuery = postgresExportQuery;
    copyManagerSupplier = new DefaultCopyManagerSupplier();
  }

  /**
   * Exports Postgres database data using {@link CopyManager} into {@link DatabaseExportDestination}
   * implementation. Implementation will close destination when export operation is finished.
   *
   * @param destination {@link DatabaseExportDestination} into which data will be exported.
   * @return Full link to stored export data.
   */
  @Override
  public URI exportTo(DatabaseExportDestination destination) {
    try (Connection poolManagedConnection = dataSource.getConnection()) {
      // Note the unwrapped connection is passed to copy manager. The Hikari
      // pool managed connection is in the scope of the try
      // with resource block, so that it is returned to the pool. The unwrapped
      // connection is not closed as this is delegated
      // to Hikari.
      Connection unwrappedConnection =
          (Connection) poolManagedConnection.unwrap(PGConnection.class);
      CopyManager copyManager = copyManagerSupplier.apply(unwrappedConnection);
      copyManager.copyOut(postgresExportQuery, destination.outputStream());
      destination.flush();
      URI result = destination.getDestinationUri();
      destination.close();
      return result;
    } catch (IOException | SQLException exception) {
      throw new DatabaseExportException("Unable to run export operation on Postgres", exception);
    }
  }
}
