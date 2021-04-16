package uk.gov.caz.vcc.dto.mod;

import java.util.Map;
import lombok.Value;
import uk.gov.caz.vcc.dto.ModVehicleDto;

/**
 * Data transfer object to house a bulk MOD status check response.
 *
 */
@Value
public class GetModVehiclesResponseDto {
  Map<String, ModVehicleDto> vehicles;
}
