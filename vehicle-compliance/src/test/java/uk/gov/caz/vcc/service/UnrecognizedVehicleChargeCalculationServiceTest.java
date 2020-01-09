package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;

@ExtendWith(MockitoExtension.class)
class UnrecognizedVehicleChargeCalculationServiceTest {

  @InjectMocks
  private UnrecognizedVehicleChargeCalculationService unrecognizedVehicleChargeCalculationService;

  private static Stream<Arguments> tariffs() {
    return Stream.of(
        Arguments.arguments(tariffWithChargeWithVehicleType(VehicleType.BUS), true),
        Arguments.arguments(tariffWithChargeWithVehicleType(VehicleType.LARGE_VAN), false),
        Arguments.arguments(tariffWithChargeWithVehicleType(VehicleType.MOTORCYCLE), false)
    );
  }

  private static TariffDetails tariffWithChargeWithVehicleType(VehicleType vehicleType) {
    TariffDetails tariff = new TariffDetails();
    tariff.setRates(Collections.singletonList(new VehicleTypeCharge(vehicleType, 5f)));

    return tariff;
  }

  @Test
  public void shouldMatchVehicleTypeCaseInsensitive() {
    //given
    String vehicleType = "bUs";
    VehicleTypeCharge vehicleTypeCharge = new VehicleTypeCharge(VehicleType.BUS, 1f);

    //then
    assertThat(unrecognizedVehicleChargeCalculationService.vehicleTypesMatches(vehicleType)
        .test(vehicleTypeCharge))
        .isTrue();
  }

  @ParameterizedTest
  @MethodSource("tariffs")
  public void shouldCheckTariffExistence(TariffDetails details, boolean tariffContainsBusCharge) {
    assertThat(unrecognizedVehicleChargeCalculationService.tariffExistsForGivenType("bus", details))
        .isEqualTo(tariffContainsBusCharge);
  }

  @ParameterizedTest
  @MethodSource("tariffs")
  public void shouldGetChargeForGivenType(TariffDetails details, boolean tariffContainsBusCharge) {
    assertThat(
        unrecognizedVehicleChargeCalculationService.chargeForGivenVehicleType("bus", details))
        .matches(vehicleTypeCharge -> vehicleTypeCharge.isPresent() == tariffContainsBusCharge);
  }
}