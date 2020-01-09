package uk.gov.caz.vcc.util;

import org.assertj.core.api.Assertions;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.dto.VehicleResultDto;

public class VccAssertions extends Assertions {

  public static VehicleResultDtoAssert assertThat(VehicleResultDto actual) {
    return new VehicleResultDtoAssert(actual);
  }

  public static CleanAirZoneEntrantAssert assertThat(CleanAirZoneEntrant actual) {
    return new CleanAirZoneEntrantAssert(actual);
  }
}