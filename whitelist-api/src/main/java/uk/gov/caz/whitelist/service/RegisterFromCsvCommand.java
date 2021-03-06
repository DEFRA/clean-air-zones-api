package uk.gov.caz.whitelist.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.UUID;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.CsvFindResult;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.repository.WhitelistedVehicleDtoCsvRepository;

/**
 * Class which is responsible for registering vehicles whose data is located at S3.
 */
public class RegisterFromCsvCommand extends AbstractRegisterCommand {

  private final String bucket;
  private final String filename;

  private final WhitelistedVehicleDtoCsvRepository csvRepository;

  private CsvFindResult csvFindResult;

  private boolean shouldPurgeFileFromS3;

  /**
   * Creates an instance of {@link RegisterFromCsvCommand}.
   */
  public RegisterFromCsvCommand(RegisterServicesContext registerServicesContext, int registerJobId,
      String correlationId, String bucket, String filename) {
    super(registerServicesContext, registerJobId, correlationId);
    this.bucket = bucket;
    this.filename = filename;
    this.csvRepository = registerServicesContext.getCsvRepository();
  }

  @Override
  public void beforeExecute() {
    csvFindResult = csvRepository.findAll(bucket, filename);
  }

  @Override
  public List<WhitelistedVehicleDto> getVehiclesToRegister() {
    checkCsvParseResultsPresentPrecondition();
    return csvFindResult.getVehicles();
  }

  @Override
  UUID getUploaderId() {
    return csvFindResult.getUploaderId();
  }

  @Override
  String getUploaderEmail() {
    return csvFindResult.getEmail();
  }

  @Override
  List<ValidationError> getParseValidationErrors() {
    checkCsvParseResultsPresentPrecondition();
    return csvFindResult.getValidationErrors();
  }

  @Override
  boolean shouldMarkJobFailed() {
    return shouldPurgeFileFromS3;
  }

  @Override
  void onBeforeMarkJobFailed() {
    shouldPurgeFileFromS3 = csvRepository.purgeFile(bucket, filename);
  }

  private void checkCsvParseResultsPresentPrecondition() {
    Preconditions.checkState(csvFindResult != null, "CSV parse results need to obtained first");
  }
}
