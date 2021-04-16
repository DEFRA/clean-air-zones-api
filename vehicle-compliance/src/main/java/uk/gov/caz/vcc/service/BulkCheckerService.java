package uk.gov.caz.vcc.service;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;

/**
 * Service layer implementation for handling CSV-based bulk vehicle chargeability checks.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Setter
public class BulkCheckerService {
  private final S3Client s3Client;
  private final ChargeCalculationService chargeCalculationService;
  private final CazTariffService tariffService;

  @Value("${application.bulk-checker.s3-bucket:jaqu.caz}")
  private String s3Bucket;
  
  @Value("${application.bulk-checker.filePrefix:}")
  private String filePrefix;

  /**
   * Process uploaded csv file.
   * @param bucket S3 bucket where the csv file is located
   * @param filename The csv filename
   * @param timeoutInSeconds Function processing timeout
   */
  public void process(String bucket, String filename, int timeoutInSeconds) {
    try {
      List<String> vrns = loadVrnsFrom(bucket, filename);
      List<CleanAirZoneDto> cleanAirZones = tariffService
          .getCleanAirZoneSelectionListings().getCleanAirZones();
      List<CsvOutput> responses = chargeCalculationService
          .getComplianceCheckAsCsv(vrns, cleanAirZones);
      persist(responses, bucket, filename);
    } catch (Exception e) {
      log.error(e.getMessage());
      log.error("Exception while processing file {}/{}", bucket, filename);
    }
  }
  
  /**
   * Get Bulk Checker output file.
   * @param fileName Csv filename under processing
   * @return Path to output Csv file located in S3
   * @throws FileNotFoundException if the under processing csv file not exists
   */
  public BulkCheckerCsvFile getBulkCheckerOutputFile(String fileName) throws FileNotFoundException {
    String fileExt = fileName.substring(fileName.indexOf('.'));
    String outputFileName = fileName.substring(0, fileName.indexOf('.'))
                                    .concat("-output").concat(fileExt);

    String outputFilePath = filePrefix.concat("output/").concat(outputFileName);
    if (s3objectExists(s3Bucket, outputFilePath)) {
      return new BulkCheckerCsvFile(s3Bucket, outputFilePath);
    } else {
      String inputFilePath = filePrefix.concat("input/").concat(fileName);
      if (s3objectExists(s3Bucket, inputFilePath)) {
        return new BulkCheckerCsvFile(s3Bucket, null);
      } else {
        throw new FileNotFoundException();
      }
    }
  }

  /**
   * Method to test whether an S3 object exists.
   * @param bucket the bucket name in which objects are stored.
   * @param key the unique key for the object being tested.
   * @return boolean indicator for whether an object exists in an S3 bucket.
   */
  private boolean s3objectExists(String bucket, String key) {
    HeadObjectRequest request =
        HeadObjectRequest.builder().bucket(bucket).key(key).build();
    try {
      s3Client.headObject(request);
      return true;
    } catch (NoSuchKeyException ex) {
      log.error("Query S3 object {}/{} exception: {}", bucket, key, ex.getMessage());
      return false;
    }
  }

  /**
   * Persists a CSV output of a bulk check result to S3.
   * @param csvOutputs a series of CSV row representations to write to the output location.
   * @param bucket the bucket to which an output will be written.
   * @param filePath the object key to be used for the output file.
   */
  private void persist(List<CsvOutput> csvOutputs, String bucket, String filePath) {

    String fileFolder = filePath.substring(0, filePath.indexOf("input"));
    String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
    String fileExt = fileName.substring(fileName.indexOf('.'));
    String newFileName = fileFolder.concat("output/").concat(
        fileName.substring(0, fileName.indexOf('.')).concat("-output").concat(fileExt));
    log.info("Writing result to {}", newFileName);

    StringBuilder builder = new StringBuilder();
    
    if (csvOutputs.size() > 1) {
      CsvOutput header = csvOutputs.get(0);
      builder.append(printCsvRow(header, true));
      for (int i = 1; i < csvOutputs.size(); i++) {
        builder.append(printCsvRow(csvOutputs.get(i), false));
      }
    }

    PutObjectRequest request =
        PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(newFileName)
                        .build();
    s3Client.putObject(request, RequestBody.fromString(builder.toString()));
  }

  /**
   * Method for creating a CSV file string representation.
   * @param line a line of data in a CSV output.
   * @param isHeader indicator whether the supplied line is a header row in a CSV output.
   * @return a string representation of a CSV file row.
   */
  private String printCsvRow(CsvOutput line, boolean isHeader) {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("%-25s,", (isHeader ? "Registration Number" : line.getVrn())));
    builder.append(String.format("%-25s,", (isHeader ? "Type" : line.getVehicleType())));
    line.getCharges()
          .entries()
          .forEach(entry -> builder.append(String.format("%10s,", entry.getValue())));
    builder.append("\r\n");
    return builder.toString();
  }

  /**
   * Method to load VRNs from an input CSV file.
   * @param bucket the bucket in which source CSV files is located.
   * @param filename the object key for the input CSV file.
   * @return a list of VRNs extracted from the input CSV file.
   */
  private List<String> loadVrnsFrom(String bucket, String filename) {
    InputStream input = openFileInputStream(bucket,filename);
    List<RemoteVehicleDataRequest> requests =
        new CsvToBeanBuilder<RemoteVehicleDataRequest>(
            new InputStreamReader(input))
                  .withType(RemoteVehicleDataRequest.class).build().parse();
    return requests.stream()
                  .map(request -> request.getRegistrationNumber().replaceAll("\\s+",""))
                  .collect(Collectors.toList());
  }
  
  /**
   * Method to open a file input stream from a source S3 object.
   * @param bucket the bucket in which source CSV files is located.
   * @param filename the object key for the input CSV file.
   * @return an input stream for the requested file.
   */
  private InputStream openFileInputStream(
      String bucket, String filename) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(filename)
                        .build();
    return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
  }

  /**
   * A CSV file representation for bulk chargeability checking purposes.
   *
   */
  @RequiredArgsConstructor
  @Getter
  public static class BulkCheckerCsvFile {
    private final String s3Bucket;
    private final String fileName;
  }
}