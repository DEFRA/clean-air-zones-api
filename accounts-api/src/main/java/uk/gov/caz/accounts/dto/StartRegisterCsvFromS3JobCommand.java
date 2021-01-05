package uk.gov.caz.accounts.dto;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Value;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

/**
 * Command class that holds data required to start registering CSV from S3 job.
 */
@Value
public class StartRegisterCsvFromS3JobCommand {

  private static final Map<Function<StartRegisterCsvFromS3JobCommand, Boolean>, String> validators =
      ImmutableMap.<Function<StartRegisterCsvFromS3JobCommand, Boolean>, String>builder()
          .put(s3BucketNotEmpty(), "'s3Bucket' cannot be null or empty")
          .put(filenameNotEmpty(), "'filename' cannot be null or empty")
      .build();

  /**
   * Name of S3 Bucket in which CSV file is stored.
   */
  @ApiModelProperty(notes = "Name of S3 Bucket in which CSV file is stored")
  String s3Bucket;

  /**
   * Name of CSV file whose content should be imported.
   */
  @ApiModelProperty(notes = "Name of CSV file whose content should be imported")
  String filename;

  /**
   * Flag indicating whether to send email(s) upon successful job completion. {@code true} is the
   * default value unless provided.
   */
  @ApiModelProperty(notes = "Flag indicating whether emails should be send upon successful "
      + "completion of the job, true by default")
  Boolean successEmail;

  /**
   * Getter for {@code successEmail} that returns either the provided value (if not null) or the
   * default one.
   */
  public boolean shouldSendEmailsUponSuccessfulJobCompletion() {
    return Objects.isNull(successEmail) || successEmail;
  }

  /**
   * Validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Validates whether 's3Bucket' is not null and not empty.
   */
  private static Function<StartRegisterCsvFromS3JobCommand, Boolean> s3BucketNotEmpty() {
    return request -> StringUtils.hasText(request.getS3Bucket());
  }

  /**
   * Validates whether 'filename' is not null and not empty.
   */
  private static Function<StartRegisterCsvFromS3JobCommand, Boolean> filenameNotEmpty() {
    return request -> StringUtils.hasText(request.getFilename());
  }
}