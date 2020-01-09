package uk.gov.caz.vcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class for single Vehicle Entrant response object.
 */
@Value
@Builder(toBuilder = true)
public class VehicleResultDto {

  /**
   * String containing the unique Vehicle Registration Number of the witnessed vehicle without
   * whitespace.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.vrn}")
  String vrn;

  /**
   * Vehicle make.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.make}")
  String make;

  /**
   * Vehicle model.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.model}")
  String model;

  /**
   * Vehicle colour.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.colour}")
  String colour;

  /**
   * Vehicle type approval category.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.typeApproval}")
  String typeApproval;

  /**
   * Returns one of the following values (exempt, compliant, notCompliantPaid, notCompliantNotPaid,
   * unrecognisedPaid, unrecognisedNotPaid).
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.status}")
  String status;

  /**
   * A code that uniquely identifies the national exemption on which the exempt status is based.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.exemptionCode}")
  String exemptionCode;

  /**
   * A code that uniquely identifies the tariff that determines the charge that the vehicle is
   * liable to pay.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.tariffCode}")
  String tariffCode;

  /**
   * Returns true if the vehicle is a taxi or PHV.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.isTaxiOrPhv}")
  boolean isTaxiOrPhv;

  /**
   * Return list of Licensing Authority that the taxi or Private Hire vehicle is licensed with.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.licensingAuthority}")
  List<String> licensingAuthority;

  @JsonProperty("isTaxiOrPhv")
  public boolean isTaxiOrPhv() {
    return isTaxiOrPhv;
  }
}