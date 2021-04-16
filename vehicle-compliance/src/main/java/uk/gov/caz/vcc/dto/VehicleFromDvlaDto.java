package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import java.text.SimpleDateFormat;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.domain.Vehicle;

/**
 * DTO for Vehicle information from DVLA.
 */
@Value
@Builder
public class VehicleFromDvlaDto {

  /**
   * Vehicle registration number.
   */
  @ApiModelProperty(
      notes = "${swagger.model.descriptions.vehicleResultFromDVLA.registrationNumber}")
  @NotNull
  String registrationNumber;

  /**
   * Vehicle colour.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.colour}")
  @NotNull
  String colour;

  /**
   * Date of the first registration of a vehicle.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA."
      + "dateOfFirstRegistration}")
  @NotNull
  String dateOfFirstRegistration;

  /**
   * Euro Status (Dealer / Customer provided).
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.euroStatus}")
  @NotNull
  String euroStatus;

  /**
   * Fuel Type (Method of Propulsion).
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.fuelType}")
  @NotNull
  String fuelType;

  /**
   * Vehicle make.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.make}")
  @NotNull
  String make;

  /**
   * Vehicle Type Approval Category.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.typeApproval}")
  @NotNull
  String typeApproval;

  /**
   * Revenue weight in kilograms.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.revenueWeight}")
  @NotNull
  Integer revenueWeight;

  /**
   * Unladen weight in kilograms.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.unladenWeight}")
  @NotNull
  Integer unladenWeight;

  /**
   * Body type of vehicle.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.bodyType}")
  @NotNull
  String bodyType;

  /**
   * Vehicle model.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.model}")
  @NotNull
  String model;

  /**
   * Seating capacity of vehicle.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.seatingCapacity}")
  @NotNull
  Integer seatingCapacity;

  /**
   * Standing capacity of vehicle.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.standingCapacity}")
  @NotNull
  Integer standingCapacity;

  /**
   * Tax class of vehicle.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResultFromDVLA.taxClass}")
  @NotNull
  String taxClass;

  /**
   * Gets registration date as a string from a vehicle.
   *
   * @param vehicle to retrieve string date from.
   */
  private static String vehicleRegistrationDate(Vehicle vehicle) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM");
    String dateString = "";
    if (null != vehicle.getDateOfFirstRegistration()) {
      dateString = dateFormatter.format(vehicle.getDateOfFirstRegistration());
    }
    return dateString;
  }

  /**
   * returns DTO from Vehicle class.
   *
   * @param vehicle object to process.
   */
  public static VehicleFromDvlaDto fromVehicle(Vehicle vehicle) {
    String dateString = vehicleRegistrationDate(vehicle);
    return VehicleFromDvlaDto.builder()
        .registrationNumber(vehicle.getRegistrationNumber())
        .colour(vehicle.getColour())
        .dateOfFirstRegistration(dateString)
        .euroStatus(vehicle.getEuroStatus())
        .fuelType(vehicle.getFuelType())
        .make(vehicle.getMake())
        .typeApproval(vehicle.getTypeApproval())
        .revenueWeight(vehicle.getRevenueWeight())
        .unladenWeight(vehicle.getMassInService())
        .bodyType(vehicle.getBodyType())
        .model(vehicle.getModel())
        .seatingCapacity(vehicle.getSeatingCapacity())
        .standingCapacity(vehicle.getStandingCapacity())
        .taxClass(vehicle.getTaxClass())
        .build();
  }
}