package uk.gov.caz.taxiregister.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Value;
import uk.gov.caz.taxiregister.model.VehicleLicenceLookupInfo;

@Value
public class LicenceInfo {

  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info.active}")
  boolean active;

  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info.description")
  String description;

  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info.wheelchair-accessible}")
  Boolean wheelchairAccessible;

  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info.licensed-status-expires}")
  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate licensedStatusExpires;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.licence-info.licensing-authorities-names}")
  List<String> licensingAuthoritiesNames;

  @ApiModelProperty(value = "${swagger.model.descriptions.licence-info.added-timestamps}")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime addedTimestamp;

  /**
   * Maps {@link VehicleLicenceLookupInfo} to {@link LicenceInfo}.
   *
   * @param lookupInfo An instance of {@link VehicleLicenceLookupInfo} to be mapped
   * @return An instance of {@link LicenceInfo} mapped from {@link VehicleLicenceLookupInfo}
   */
  public static LicenceInfo from(VehicleLicenceLookupInfo lookupInfo) {
    return new LicenceInfo(
        lookupInfo.hasAnyOperatingLicenceActive(),
        lookupInfo.getDescription(),
        lookupInfo.getWheelchairAccessible(),
        lookupInfo.getLicensedStatusExpires(),
        lookupInfo.getLicensingAuthoritiesNames(),
        lookupInfo.getAddedTimestamp()
    );
  }
}
