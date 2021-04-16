package uk.gov.caz.taxiregister.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicenceHistory;

/**
 * Class that contains historical info of licence.
 */
@Value
@Builder
public class LicenceInfoHistoricalResult {

  public static final String DATE_FORMAT = "yyyy-MM-dd";
  /**
   * ISO 8601 formatted date string indicating the modification date.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.modifyDate}")
  @JsonFormat(pattern = DATE_FORMAT)
  LocalDate modifyDate;

  /**
   * Status of current VRM for a specific date range.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.action}")
  String action;

  /**
   * LA name.
   */
  @ApiModelProperty(
      value = "${swagger.model.descriptions.licence-info-history.licensingAuthorityName}")
  String licensingAuthorityName;

  /**
   * A vehicle registration plate.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.plateNumber}")
  String plateNumber;

  /**
   * ISO 8601 formatted date string indicating the date from which a license is valid.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.licenceStartDate}")
  @JsonFormat(pattern = DATE_FORMAT)
  LocalDate licenceStartDate;

  /**
   * ISO 8601 formatted date string indicating the date until which a license is valid.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info-history.licenceEndDate}")
  @JsonFormat(pattern = DATE_FORMAT)
  LocalDate licenceEndDate;

  /**
   * Boolean to indicate whether a given vehicle is wheelchair accessible.
   */
  @ApiModelProperty(
      value = "${swagger.model.descriptions.licence-info-history.wheelchairAccessible}")
  Boolean wheelchairAccessible;

  /**
   * Maps {@link TaxiPhvVehicleLicenceHistory} to {@link LicenceInfoHistoricalResult}.
   *
   * @param licenceHistory An instance of {@link TaxiPhvVehicleLicenceHistory} to be mapped
   * @return An instance of {@link LicenceInfoHistoricalResult} mapped from {@link
   *     TaxiPhvVehicleLicenceHistory}
   */
  public static LicenceInfoHistoricalResult from(
      TaxiPhvVehicleLicenceHistory licenceHistory) {
    return LicenceInfoHistoricalResult.builder()
        .modifyDate(licenceHistory.getModifyDate())
        .action(licenceHistory.getAction())
        .licensingAuthorityName(licenceHistory.getLicensingAuthorityName())
        .plateNumber(licenceHistory.getLicencePlateNumber())
        .licenceStartDate(licenceHistory.getLicenceStartDate())
        .licenceEndDate(licenceHistory.getLicenceEndDate())
        .wheelchairAccessible(licenceHistory.getWheelchairAccessible())
        .build();
  }
}