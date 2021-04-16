package uk.gov.caz.vcc.dto;

import java.util.List;
import java.util.Set;
import lombok.Value;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;

@Value
public class PreFetchedDataResults {
  List<GeneralWhitelistVehicle> matchedGeneralWhitelistVehicles;
  List<RetrofittedVehicle> matchedRetrofittedVehicles;
  Set<String> matchedMilitaryVrns;
}