package uk.gov.caz.db.exporter.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.postgresql.jdbc.PgConnection;
import uk.gov.caz.db.exporter.DatabaseExportDestination;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

public class PostgresDatabaseExporterTest {

  private final static String EXPORT_QUERY = "SOME POSTGRES EXPORT QUERY";
  private final static URI EXPORT_PATH = URI.create("some_uri");
  private DatabaseExportDestination destination;
  private CopyManager copyManager;
  private PostgresDatabaseExporter postgresExporter;
  private DataSource dataSource;
  private Connection connection;
  private BaseConnection pgConnection;
  
  @BeforeEach
  public void setup() throws SQLException {
    destination = mock(DatabaseExportDestination.class);
    when(destination.outputStream()).thenReturn(System.out);

    pgConnection = mock(BaseConnection.class);
    connection = mock(Connection.class);
    dataSource = mock(DataSource.class);
    
    copyManager = mock(CopyManager.class);
    postgresExporter = new PostgresDatabaseExporter(dataSource, EXPORT_QUERY);
    postgresExporter.setCopyManagerSupplier(connection -> copyManager);
  }

  @Test
  public void successfulPostgresExportingDelegatesToDestinationInCorrectOrderWithPostgresWrapper()
      throws IOException, SQLException {
    
    // given
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isWrapperFor(PGConnection.class)).thenReturn(true);
    when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
    
    when(destination.getDestinationUri()).thenReturn(EXPORT_PATH);

    // when
    URI exportPath = postgresExporter.exportTo(destination);

    // then
    assertThat(exportPath).isEqualTo(EXPORT_PATH);
    verify(destination).outputStream();
    verify(destination).flush();
    verify(destination).close();
    verify(copyManager).copyOut(eq(EXPORT_QUERY), any(OutputStream.class));
  }
  
  @Test
  public void successfulPostgresExportingDelegatesToDestinationInCorrectOrder()
      throws IOException, SQLException {
    
    // given
    when(dataSource.getConnection()).thenReturn(connection);
    when(destination.getDestinationUri()).thenReturn(EXPORT_PATH);

    // when
    URI exportPath = postgresExporter.exportTo(destination);

    // then
    assertThat(exportPath).isEqualTo(EXPORT_PATH);
    verify(destination).outputStream();
    verify(destination).flush();
    verify(destination).close();
    verify(copyManager).copyOut(eq(EXPORT_QUERY), any(OutputStream.class));
  }

  @Test
  public void exceptionDuringPostgresExportingProducesOurCustomException()
      throws IOException, SQLException {
    // given
    when(connection.isWrapperFor(PgConnection.class)).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(connection);
    when(copyManager.copyOut(EXPORT_QUERY, System.out)).thenThrow(new SQLException());

    // when
    Throwable throwable = catchThrowable(
        () -> postgresExporter.exportTo(destination));

    // then
    assertThat(throwable).isInstanceOf(DatabaseExportException.class)
        .hasMessage("Unable to run export operation on Postgres").hasCause(new SQLException());
  }
}
