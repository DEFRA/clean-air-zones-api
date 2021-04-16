package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

/**
 * Wrapper class for single Vehicle Entrant request object.
 */
@Value
@Builder
@AllArgsConstructor
public class VehicleEntrantSaveDto {

  /**
   * String containing unique Vehicle registration number.
   */
  @ToString.Exclude
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleEntrants.vrn}")
  @NotNull
  @Size(min = 2, max = 15)
  String vrn;

  /**
   * ISO-8601 formatted datetime indicating  when the vehicle was witnessed entering the CAZ.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleEntrants.timestamp}")
  @NotNull
  @Size(min = 18, max = 25)
  LocalDateTime timestamp;

  /**
   * Method creates {@link VehicleEntrantSaveDto} from the provided {@link VehicleEntrantDto} with
   * properly parsed timestamp.
   */
  public static VehicleEntrantSaveDto from(VehicleEntrantDto vehicleEntrant,
      DateTimeFormatter dateTimeFormatter) {
    return VehicleEntrantSaveDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .timestamp(LocalDateTime.parse(vehicleEntrant.getTimestamp(), dateTimeFormatter))
        .build();
  }
}
