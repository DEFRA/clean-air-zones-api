package uk.gov.caz.vcc.dto.mod;

import java.util.List;
import lombok.Value;

/**
 * Data transfer object for requesting the MOD status for a collection
 * of registration numbers.
 *
 */
@Value(staticConstructor = "of")
public class GetModVehiclesRequestDto {
  List<String> vrns;
}
