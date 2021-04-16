package uk.gov.caz.vcc.dto.ntr;

import java.util.List;
import lombok.Value;

/**
 * Data transfer object for requesting the taxi license status for a collection
 * of registration numbers.
 *
 */
@Value
public class GetLicencesInfoRequestDto {
  List<String> vrms;
}
