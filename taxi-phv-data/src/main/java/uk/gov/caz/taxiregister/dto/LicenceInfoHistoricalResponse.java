package uk.gov.caz.taxiregister.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class that contains {@link List} of {@link LicenceInfoHistoricalResult} and page details.
 */
@Value
@Builder
public class LicenceInfoHistoricalResponse {

  /**
   * Page that has been retrieved.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.page}")
  long page;

  /**
   * Total number of pages available (with current page size).
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.pageCount}")
  long pageCount;

  /**
   * The current page size.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.perPage}")
  long perPage;

  /**
   * The total number of changes associated with this vehicle.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.totalChangesCount}")
  long totalChangesCount;

  /**
   * A list of changes associated with this vehicle.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.changes}")
  List<LicenceInfoHistoricalResult> changes;
}