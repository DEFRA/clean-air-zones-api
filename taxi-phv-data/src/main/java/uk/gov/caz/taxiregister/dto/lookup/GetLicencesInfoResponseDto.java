package uk.gov.caz.taxiregister.dto.lookup;

import java.util.Map;
import lombok.Value;
import uk.gov.caz.taxiregister.dto.LicenceInfo;

@Value(staticConstructor = "from")
public class GetLicencesInfoResponseDto {
  Map<String, LicenceInfo> licencesInformation;
}
