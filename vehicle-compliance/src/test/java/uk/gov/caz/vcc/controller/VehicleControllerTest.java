package uk.gov.caz.vcc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.dto.VehicleDto;
import uk.gov.caz.vcc.dto.validation.InvalidCazZoneFormat;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;
import uk.gov.caz.vcc.service.ChargeCalculationService;
import uk.gov.caz.vcc.service.VehicleService;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

  String vrn;
  Vehicle vehicle;

  @Mock
  private VehicleService vehicleService;

  @Mock
  private ChargeCalculationService chargeCalculationService;

  @Mock
  private VehicleDetailsRepository vehicleDetailsRepository;

  @Mock
  private FuelTypeService fuelTypeService;

  @InjectMocks
  private VehicleController vehicleController;

  @BeforeEach
  void setUp() {
    this.vrn = "CU12346";
    this.vehicle = new Vehicle();
  }

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

  private VehicleDto someVehicle(String someRegistrationNumber,
      boolean isTaxi) {
    return VehicleDto.builder().registrationNumber(someRegistrationNumber)
        .isTaxiOrPhv(isTaxi).build();
  }

  @Test
  void isNotExemptReturnOk() throws Exception {
    // given
    Mockito
        .when(chargeCalculationService.checkVrnAgainstCaz(Mockito.anyString(),
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
  void isExemptReturnOk() throws Exception {
    // given
    Mockito
        .when(chargeCalculationService.checkVrnAgainstCaz(Mockito.anyString(),
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
}