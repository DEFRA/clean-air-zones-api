package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import java.net.URL;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents JSON response after csv export.
 */
@Value
@Builder
public class CsvExportResponse {

  /**
   * Presigned url.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.csv-export.fileUrl}")
  URL fileUrl;

  /**
   * Bucket name.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.csv-export.bucketName")
  String bucketName;
}