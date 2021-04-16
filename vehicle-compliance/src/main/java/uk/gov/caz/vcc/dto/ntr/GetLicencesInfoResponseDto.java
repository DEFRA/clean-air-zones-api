package uk.gov.caz.vcc.dto.ntr;

import java.util.Map;
import lombok.Value;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

/**
 * Data transfer object to house a bulk taxi status check response.
 *
 */
@Value
public class GetLicencesInfoResponseDto {
  Map<String, TaxiPhvLicenseInformationResponse> licencesInformation;
}
