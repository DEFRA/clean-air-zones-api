package uk.gov.caz.definitions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.domain.Vehicle;

/*
 * Value object that holds information about vehicle details.
 */
@Value
@Builder(toBuilder = true)
public class VehicleDto {

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.registrationNumber}")
  String registrationNumber;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.typeApproval}")
  String typeApproval;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.type}")
  String type;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.make}")
  String make;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.model}")
  String model;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.colour}")
  String colour;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.fuelType}")
  String fuelType;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.isTaxiOrPhv}")
  @JsonProperty("taxiOrPhv")
  boolean isTaxiOrPhv;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.isExempt}")
  @JsonProperty("exempt")
  boolean isExempt;

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.licensingAuthoritiesNames}")
  private List<String> licensingAuthoritiesNames;

  /**
   * Helper method to map DTO object from inner domain vehicle representation.
   *
   * @param vehicle domain object representation of a vehicle.
   * @param isExempt indicator for whether a vehicle has been deemed exempt from charges.
   * @return DTO representation of a vehicle.
   * @throws NotFoundException If the fuel type cannot be converted throw NotFound.
   */
  public static VehicleDto fromVehicle(Vehicle vehicle, boolean isExempt) {

    return VehicleDto.builder().registrationNumber(vehicle.getRegistrationNumber())
        .typeApproval(Optional.ofNullable(vehicle.getTypeApproval()).orElse(""))
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .type(String.valueOf(vehicle.getVehicleType()))
        .fuelType(vehicle.getFuelType())
        .isTaxiOrPhv(Optional.ofNullable(vehicle.getIsTaxiOrPhv()).orElse(false))
        .licensingAuthoritiesNames(
            Optional.ofNullable(vehicle.getLicensingAuthoritiesNames()).orElse(null))
        .isExempt(isExempt)
        .build();
  }

}