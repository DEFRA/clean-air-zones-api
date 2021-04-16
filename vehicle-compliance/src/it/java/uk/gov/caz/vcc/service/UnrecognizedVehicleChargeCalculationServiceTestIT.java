package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
class UnrecognizedVehicleChargeCalculationServiceTestIT extends MockServerTestIT {

  private static final float CHARGE_FOR_CAR_IN_BATH = 15.5f;
  private static final float CHARGE_FOR_CAR_IN_BIRMINGHAM = 10.5f;
  public static final String BATH_TARIFF_CODE = "BTH01-PRIVATE_CAR";
  public static final String BIRMINGHAM_TARIFF_CODE = "BCC01-PRIVATE_CAR";
  private static String BATH_CAZ = "131af03c-f7f4-4aef-81ee-aae4f56dbeb5";
  private static String BIRMINGHAM_CAZ = "0d7ab5c4-5fff-4935-8c4e-56267c0c9493";

  @Autowired
  private UnrecognizedVehicleChargeCalculationService unrecognizedVehicleChargeCalculationService;

  @Test
  public void shouldTestFetchingChargesForGivenVehicleForGivenCazes() {
    //given
    String vehicleType = "private_car";

    //when
    whenCazInfoIsInTariffService();
    whenEachCazHasTariffInfo();

    //then
    thenOnlyTariffsForVehicleTypeShouldBeFetchedFromZones(vehicleType);
  }

  private void thenOnlyTariffsForVehicleTypeShouldBeFetchedFromZones(String vehicleType) {
    //given
    ComplianceOutcomeDto expectedChargeInBath = ComplianceOutcomeDto.builder()
        .cleanAirZoneId(UUID.fromString(BATH_CAZ))
        .charge(CHARGE_FOR_CAR_IN_BATH)
        .tariffCode(BATH_TARIFF_CODE)
        .name("Bath")
        .build();

    ComplianceOutcomeDto expectedChargeInBirmingham = ComplianceOutcomeDto.builder()
        .cleanAirZoneId(UUID.fromString(BIRMINGHAM_CAZ))
        .charge(CHARGE_FOR_CAR_IN_BIRMINGHAM)
        .tariffCode(BIRMINGHAM_TARIFF_CODE)
        .name("Birmingham")
        .build();

    //when
    VehicleTypeCazChargesDto charges = unrecognizedVehicleChargeCalculationService.getCharges(
        vehicleType,
        Lists.newArrayList(UUID.fromString(BATH_CAZ), UUID.fromString(BIRMINGHAM_CAZ))
    );

    //then
    assertThat(charges.getCharges())
        .containsExactlyInAnyOrder(expectedChargeInBirmingham, expectedChargeInBath);
  }

}