package uk.gov.caz.db.exporter.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

/**
 * Provides default Postgres {@link CopyManager} implementation. {@link PostgresDatabaseExporter}
 * uses it by default until changed by a setter.
 */
class DefaultCopyManagerSupplier implements Function<Connection, CopyManager> {

  @Override
  public CopyManager apply(Connection connection) {
    try {
      BaseConnection baseConnection = (BaseConnection) (connection.getMetaData().getConnection());

      return new CopyManager(baseConnection);
    } catch (SQLException sqlException) {
      throw new DatabaseExportException(
          "Unable to run export operation on Postgres due to the problems in obtaining "
              + "Postgres connection", sqlException);
    }
  }
}
