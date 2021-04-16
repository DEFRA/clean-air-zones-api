package uk.gov.caz.whitelist.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Value;
import uk.gov.caz.whitelist.model.WhitelistVehicle;

/**
 * Value object that represents whitelisted vehicle details for get endpoint.
 */
@Value
public class WhitelistedVehicleDetailsResponseDto {
  /**
   * String containing vehicle registration number.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.vrn}")
  String vrn;

  /**
   * String containing category.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.category}")
  String category;

  /**
   * Reason given in csv file.
   */
  @ApiModelProperty(value
      = "${swagger.model.descriptions.whitelisted-vehicle-details.reasonUpdated}")
  String reasonUpdated;

  /**
   * Date of update row.
   */
  @ApiModelProperty(value
      = "${swagger.model.descriptions.whitelisted-vehicle-details.updateTimestamp}")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime updateTimestamp;

  /**
   * Date of insert  row.
   */
  @ApiModelProperty(value
      = "${swagger.model.descriptions.whitelisted-vehicle-details.addedTimestamp}")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime addedTimestamp;

  /**
   * User's sub.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.uploaderId}")
  UUID uploaderId;

  /**
   * User's email.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.email}")
  String email;

  /**
   * String containing a company, which build vehicle.
   */
  @ApiModelProperty(value
      = "${swagger.model.descriptions.whitelisted-vehicle-details.manufacturer}")
  String manufacturer;

  /**
   * Boolean value specifying if vehicle is exempt.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.exempt}")
  boolean exempt;

  /**
   * Boolean value specifying if vehicle is compliant.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.compliant}")
  boolean compliant;

  /**
   * Maps {@link WhitelistVehicle} to {@link WhitelistedVehicleDetailsResponseDto}.
   *
   * @param whitelistVehicle An instance of {@link WhitelistVehicle} to be mapped
   * @return An instance of {@link WhitelistedVehicleDetailsResponseDto} mapped from {@link
   *     WhitelistVehicle}
   */
  public static WhitelistedVehicleDetailsResponseDto from(WhitelistVehicle whitelistVehicle) {
    return new WhitelistedVehicleDetailsResponseDto(
        whitelistVehicle.getVrn(),
        whitelistVehicle.getCategory(),
        whitelistVehicle.getReasonUpdated(),
        whitelistVehicle.getUpdateTimestamp(),
        whitelistVehicle.getInsertTimestamp(),
        whitelistVehicle.getUploaderId(),
        whitelistVehicle.getUploaderEmail(),
        whitelistVehicle.getManufacturer().orElse(null),
        whitelistVehicle.isExempt(),
        whitelistVehicle.isCompliant()
    );
  }

}
