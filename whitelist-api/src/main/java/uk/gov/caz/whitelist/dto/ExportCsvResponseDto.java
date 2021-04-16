package uk.gov.caz.whitelist.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Value object that represents details of an exported csv file.
 */
@Value
@Builder
public class ExportCsvResponseDto {

  /**
   * String containing the url to the s3 file.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.export-csv.fileUrl}")
  String fileUrl;

  /**
   * String containing the bucket name.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.export-csv.bucketName}")
  String bucketName;
}