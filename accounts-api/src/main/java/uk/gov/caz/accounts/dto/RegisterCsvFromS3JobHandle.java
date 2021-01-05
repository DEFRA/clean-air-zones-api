package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Value;

/**
 * Class that represents a handle that uniquely identifies job that registers CSV file from S3.
 */
@Value(staticConstructor = "of")
public class RegisterCsvFromS3JobHandle {

  /**
   * Name that uniquely identifies job of registering contents of a CSV file from S3 into
   * the database.
   */
  @ApiModelProperty(notes = "Name that uniquely identifies job of registering CSV from S3")
  @NotNull
  @NotBlank
  String jobName;
}
