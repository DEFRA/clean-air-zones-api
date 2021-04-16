package uk.gov.caz.whitelist.configuration;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.db.exporter.postgresql.PostgresDatabaseExporter;

/**
 * Provides beans needed to export Whitelist Vehicles into AWS S3.
 */
@Configuration
public class DbExporterConfiguration {

  /**
   * Creates Spring Bean that allows to export data from Postgres 'T_WHITELIST_VEHICLES' table as
   * CSV into any destination selected at call point.
   *
   * @param dataSource {@link DataSource} with database connections.
   * @return {@link PostgresDatabaseExporter} instance.
   */
  @Bean
  public PostgresDatabaseExporter whitelistVehiclesPostgresCsvExporter(DataSource dataSource) {
    return new PostgresDatabaseExporter(dataSource, postgresCopyWhitelistVehiclesSortedByVrn());
  }

  /**
   * Postgres COPY query that selects and streams Whitelist Vehicles sorted by VRN.
   */
  private String postgresCopyWhitelistVehiclesSortedByVrn() {
    return "COPY "
        + "(SELECT VRN AS \"VRN\", CATEGORY AS \"Category\", "
        + "REASON_UPDATED AS \"Reason Updated\", MANUFACTURER AS \"Manufacturer\" "
        + "FROM CAZ_WHITELIST_VEHICLES.T_WHITELIST_VEHICLES "
        + "ORDER BY VRN ASC) "
        + "TO STDOUT "
        + "WITH (FORMAT csv, HEADER)";
  }
}