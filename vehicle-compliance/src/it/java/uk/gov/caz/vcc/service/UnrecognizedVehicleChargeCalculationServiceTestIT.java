package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.dto.ChargeDto;
import uk.gov.caz.vcc.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
class UnrecognizedVehicleChargeCalculationServiceTestIT extends MockServerTestIT {

  private static final float CHARGE_FOR_CAR_IN_LEEDS = 15.5f;
  private static final float CHARGE_FOR_CAR_IN_BIRMINGHAM = 10.5f;
  private static String LEEDS_CAZ = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
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
    ChargeDto expectedChargeInLeeds = new ChargeDto(UUID.fromString(LEEDS_CAZ), "Leeds",
        CHARGE_FOR_CAR_IN_LEEDS);
    ChargeDto expectedChargeInBirmingham = new ChargeDto(UUID.fromString(BIRMINGHAM_CAZ), "Birmingham",
        CHARGE_FOR_CAR_IN_BIRMINGHAM);

    //when
    VehicleTypeCazChargesDto charges = unrecognizedVehicleChargeCalculationService.getCharges(
        vehicleType,
        Lists.newArrayList(UUID.fromString(LEEDS_CAZ), UUID.fromString(BIRMINGHAM_CAZ))
    );

    //then
    assertThat(charges.getCharges())
        .containsExactlyInAnyOrder(expectedChargeInBirmingham, expectedChargeInLeeds);
  }

  private void whenEachCazHasTariffInfo() {
    mockTariffCall(BIRMINGHAM_CAZ, "tariff-rates-first-response.json");
    mockTariffCall(LEEDS_CAZ, "tariff-rates-second-response.json");
  }

  private void whenCazInfoIsInTariffService() {
    mockServer.when(requestGet("/v1/clean-air-zones"))
        .respond(response("caz-first-response.json"));
  }

  private void mockTariffCall(String cazId, String file) {
    mockServer.when(requestGet("/v1/clean-air-zones/" + cazId + "/tariff"))
        .respond(response(file));
  }
}