package uk.gov.caz.vcc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.vcc.dto.VehicleFromDvlaDto;
import uk.gov.caz.vcc.dto.validation.InvalidCazZoneFormat;
import uk.gov.caz.vcc.service.CazTariffService;
import uk.gov.caz.vcc.service.ChargeCalculationService;
import uk.gov.caz.vcc.service.VehicleService;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

  private String vrn = "CU12346";
  private Vehicle vehicle = new Vehicle();
  private static final UUID CAZ_1_ID = UUID.randomUUID();
  private static final UUID CAZ_2_ID = UUID.randomUUID();

  @Mock
  private VehicleService vehicleService;

  @Mock
  private ChargeCalculationService chargeCalculationService;

  @Mock
  private CazTariffService cazTariffService;

  @InjectMocks
  private VehicleController vehicleController;

  @Test
  void shouldReturnVehicleDetailsAndStatusOK() {
    // given
    String someRegistrationNumber = "CU12345";
    boolean isTaxiOrPhv = true;
    given(vehicleService.findVehicle(someRegistrationNumber)).willReturn(
        Optional.of(someVehicle(someRegistrationNumber, isTaxiOrPhv)));

    // when
    ResponseEntity<VehicleDto> response = vehicleController
        .details(someRegistrationNumber);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getBody().getRegistrationNumber())
        .isEqualTo(someRegistrationNumber);
    assertThat(response.getBody().isTaxiOrPhv()).isEqualTo(isTaxiOrPhv);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }


  @Test
  void isNotExemptReturnOk() {
    // given
    when(chargeCalculationService.checkVrnAgainstCaz(Mockito.anyString(),
        Mockito.anyList()))
        .thenReturn(ComplianceResultsDto.builder().isExempt(false).build());

    // when
    ResponseEntity<ComplianceResultsDto> response = vehicleController
        .compliance(vrn, "d579cdd9-69b6-4099-9257-3b91573d763d");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getBody().getIsExempt()).isFalse();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void isExemptReturnOk() {
    // given
    when(chargeCalculationService.checkVrnAgainstCaz(Mockito.anyString(),
        Mockito.anyList()))
        .thenReturn(ComplianceResultsDto.builder().isExempt(true).build());

    // when
    ResponseEntity<ComplianceResultsDto> response = vehicleController
        .compliance(vrn, "d579cdd9-69b6-4099-9257-3b91573d763d");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getBody().getIsExempt()).isTrue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void forEmptyZonesRequestParameterReturnsResultsForAllZones() {
    // given
    mockTariffServiceToReturnListOfAllCazes();

    when(chargeCalculationService.checkVrnAgainstCaz(Mockito.anyString(),
        eq(allCazes())))
        .thenReturn(ComplianceResultsDto.builder().isExempt(true).build());

    // when
    ResponseEntity<ComplianceResultsDto> response = vehicleController
        .compliance(vrn, null);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getBody().getIsExempt()).isTrue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldParseValidCazId() {
    //given
    String rawCazId1 = "d579cdd9-69b6-4099-9257-3b91573d763d";
    UUID cazId2 = UUID.randomUUID();

    String validCazIds = rawCazId1 + "," + cazId2;
    List<UUID> expected =
        newArrayList(cazId2, UUID.fromString("d579cdd9-69b6-4099-9257-3b91573d763d"));

    //when
    List<UUID> result = VehicleController.parseZones(validCazIds);

    //then
    assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  void shouldThrowExceptionIfCaIdIsInvalid() {
    //given
    String invalidCazId = "XXXXXXXXX";

    //then
    assertThrows(InvalidCazZoneFormat.class,
        () -> VehicleController.parseZones(invalidCazId));
  }

  @Test
  void shouldReturnDvlaDataWhenDVLADataIsFound() {
    //given
    this.vehicle.setColour("Brown");
    given(vehicleService.dvlaDataForVehicle(this.vrn)).willReturn(Optional.of(this.vehicle));

    // when
    ResponseEntity<VehicleFromDvlaDto> dvlaData = vehicleController.dvlaData(this.vrn);

    // then
    assertThat(dvlaData.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dvlaData.getBody().getColour()).isEqualTo("Brown");
  }

  @Test
  void shouldReturn404WhenDVLADataIsMissing() {
    //given
    given(vehicleService.dvlaDataForVehicle(this.vrn)).willReturn(Optional.empty());

    // when
    ResponseEntity<VehicleFromDvlaDto> dvlaData = vehicleController.dvlaData(this.vrn);

    // then
    assertThat(dvlaData.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private VehicleDto someVehicle(String someRegistrationNumber,
      boolean isTaxi) {
    return VehicleDto.builder().registrationNumber(someRegistrationNumber)
        .isTaxiOrPhv(isTaxi).build();
  }

  private void mockTariffServiceToReturnListOfAllCazes() {
    List<CleanAirZoneDto> cleanAirZoneDtoList = newArrayList(
        CleanAirZoneDto.builder().cleanAirZoneId(CAZ_1_ID).build(),
        CleanAirZoneDto.builder().cleanAirZoneId(CAZ_2_ID).build());

    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(cleanAirZoneDtoList).build();

    when(cazTariffService.getCleanAirZoneSelectionListings()).thenReturn(cleanAirZonesDto);
  }

  private ArrayList<UUID> allCazes() {
    return newArrayList(CAZ_1_ID, CAZ_2_ID);
  }
}