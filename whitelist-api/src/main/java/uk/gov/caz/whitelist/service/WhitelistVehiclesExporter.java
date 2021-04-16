package uk.gov.caz.whitelist.service;

import com.google.common.base.Stopwatch;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.db.exporter.destination.s3.AwsS3Destination;
import uk.gov.caz.db.exporter.destination.s3.AwsS3DestinationProvider;
import uk.gov.caz.db.exporter.destination.s3.AwsS3DestinationUriGenerationStrategy;
import uk.gov.caz.db.exporter.postgresql.PostgresDatabaseExporter;

/**
 * Service that allows to export Whitelist Vehicles into AWS S3.
 */
@Service
@Slf4j
public class WhitelistVehiclesExporter {

  private static final String WHITELIST_VEHICLES_FILE_PREFIX = "whitelist_vehicles";
  private static final String WHITELIST_VEHICLES_FILE_EXT = "csv";
  private static final String CSV_MIME_TYPE = "text/csv";
  private static final String FORMATTER_PATTERN_DATETTIME = "yyyy-MM-dd_HHmmss";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern(FORMATTER_PATTERN_DATETTIME);

  private final PostgresDatabaseExporter whitelistVehiclesPostgresCsvExporter;
  private final AwsS3DestinationProvider awsS3DestinationProvider;
  private final AwsS3DestinationUriGenerationStrategy presignedUriGenerator;
  @Getter
  private final String s3DestinationBucket;

  /**
   * Initializes new instance of {@link WhitelistVehiclesExporter} class.
   *
   * @param whitelistVehiclesPostgresCsvExporter A database exporter that will export Whitelist
   *     Vehicles as CSV into specified destination.
   * @param awsS3DestinationProvider A provider that will create instances of destination set to
   *     AWS S3.
   * @param presignedUriGenerator A generator of target S3 URIs using presigned capabilities.
   * @param s3DestinationBucket A S3 bucket that will host exported CSV files.
   */
  public WhitelistVehiclesExporter(
      PostgresDatabaseExporter whitelistVehiclesPostgresCsvExporter,
      AwsS3DestinationProvider awsS3DestinationProvider,
      AwsS3DestinationUriGenerationStrategy presignedUriGenerator,
      @Value("${csv-export.bucket}") String s3DestinationBucket) {
    this.whitelistVehiclesPostgresCsvExporter = whitelistVehiclesPostgresCsvExporter;
    this.awsS3DestinationProvider = awsS3DestinationProvider;
    this.presignedUriGenerator = presignedUriGenerator;
    this.s3DestinationBucket = s3DestinationBucket;
  }

  /**
   * Exports Whitelist Vehicles as CSV into AWS S3.
   *
   * @return {@link String} with URI pointing to S3 object with exported CSV data.
   */
  public String export() {
    log.info("Starting export of Whitelist Vehicles as CSV into AWS S3");
    Stopwatch timer = Stopwatch.createStarted();
    AwsS3Destination awsS3Destination = awsS3DestinationProvider.provide()
        .inS3Bucket(s3DestinationBucket)
        .inS3Object(generateS3ObjectName())
        .withMimeType(CSV_MIME_TYPE)
        .usingUriGenerator(presignedUriGenerator)
        .inDestination();
    
    String exportedDataPath = whitelistVehiclesPostgresCsvExporter.exportTo(awsS3Destination)
        .toString();
    log.info("Exporting Whitelist Vehicles into AWS S3 took {} ms",
        timer.stop().elapsed(TimeUnit.MILLISECONDS));
    return exportedDataPath;
  }

  /**
   * Generates S3 export file names. An example 'whitelist_vehicles_2020-04-29_1038.csv'
   */
  private String generateS3ObjectName() {
    return WHITELIST_VEHICLES_FILE_PREFIX + "_" + LocalDateTime.now().format(DATE_TIME_FORMATTER)
        + "." + WHITELIST_VEHICLES_FILE_EXT;
  }
}
