package uk.gov.caz.vcc.dto;

import java.util.List;
import java.util.UUID;
import lombok.Value;

/**
 * DTO that wraps data required by service layer to perform business logic.
 */
@Value
public class VehicleEntrantsSaveRequestDto {
  private UUID cazId;
  private String correlationId;
  List<VehicleEntrantDto> vehicleEntrants;
}
