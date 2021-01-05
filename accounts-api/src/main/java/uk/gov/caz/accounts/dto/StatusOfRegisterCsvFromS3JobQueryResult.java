package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Value;
import uk.gov.caz.accounts.model.registerjob.RegisterJobError;
import uk.gov.caz.accounts.model.registerjob.RegisterJobErrors;

/**
 * Provides status information about register job: whether it is running or finished, if there were
 * some errors and their details.
 */
@Value
public class StatusOfRegisterCsvFromS3JobQueryResult {
  /**
   * Creates an instance of {@link StatusOfRegisterCsvFromS3JobQueryResult} where {@code status} is
   * set to {@code registerJobStatusDto} and no errors are found.
   */
  public static StatusOfRegisterCsvFromS3JobQueryResult withStatusAndNoErrors(
      RegisterJobStatusDto registerJobStatusDto) {
    return new StatusOfRegisterCsvFromS3JobQueryResult(registerJobStatusDto, null);
  }

  /**
   * Creates an instance of {@link StatusOfRegisterCsvFromS3JobQueryResult} where {@code status} is
   * set to {@code registerJobStatusDto} and {@code errors} are mapped to an array of {@link
   * RegisterJobError#getDetail()}.
   */
  public static StatusOfRegisterCsvFromS3JobQueryResult withStatusAndErrors(
      RegisterJobStatusDto registerJobStatusDto, RegisterJobErrors registerJobErrors) {
    String[] errorsArray = registerJobErrors.getErrors().stream()
        .map(RegisterJobError::getDetail)
        .toArray(String[]::new);
    return new StatusOfRegisterCsvFromS3JobQueryResult(registerJobStatusDto, errorsArray);
  }

  /**
   * Status code of register job identified by name.
   */
  @ApiModelProperty(
      notes = "Status code of register job identified by name",
      allowableValues = "RUNNING, "
          + "CHARGEABILITY_CALCULATION_IN_PROGRESS, "
          + "SUCCESS,  "
          + "FAILURE"
  )
  @NotNull
  RegisterJobStatusDto status;

  /**
   * List of any errors that happened during job processing. They are supposed to be displayed to
   * the end user.
   */
  @ApiModelProperty(
      notes =
          "List of any errors that happened during job processing. They are supposed to be "
              + "displayed to the end user"
  )
  String[] errors;
}
