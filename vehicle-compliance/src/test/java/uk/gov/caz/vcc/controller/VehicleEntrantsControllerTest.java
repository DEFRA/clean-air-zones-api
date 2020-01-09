package uk.gov.caz.vcc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.caz.vcc.service.VehicleEntrantsService.DATE_TIME_FORMATTER;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.caz.vcc.controller.VehicleEntrantsController.VehicleEntrantsListNullException;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsDto;
import uk.gov.caz.vcc.service.VehicleEntrantsService;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantsControllerTest {

  @Mock
  private VehicleEntrantsService vehicleEntrantsService;

  private VehicleEntrantsController vehicleEntrantsController;

  private VehicleEntrantsDto vehicleEntrantsDto;

  private VehicleEntrantDto vehicleEntrantDto;

  @BeforeEach
  void setUp() {
    vehicleEntrantsController = new VehicleEntrantsController(vehicleEntrantsService, 5);
  }

  @Test
  void shouldReturnProperResponse() {
    //given
    vehicleEntrantDto = new VehicleEntrantDto("any",
        "2017-10-01T155300Z");
    vehicleEntrantsDto = new VehicleEntrantsDto(Collections.singletonList(vehicleEntrantDto));
    //when
    ResponseEntity responseEntity = vehicleEntrantsController
        .vehicleEntrant(vehicleEntrantsDto, "any", UUID.randomUUID().toString());
    //then
    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldFailOnValidation() {
    //given
    vehicleEntrantDto = new VehicleEntrantDto("any",
        "any");
    vehicleEntrantsDto = new VehicleEntrantsDto(Collections.singletonList(vehicleEntrantDto));
    //when
    ResponseEntity responseEntity = vehicleEntrantsController
        .vehicleEntrant(vehicleEntrantsDto, "any", UUID.randomUUID().toString());
    //then
    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldFailOnPreCondition() {
    vehicleEntrantsDto = new VehicleEntrantsDto(  null);
    assertThrows(VehicleEntrantsListNullException.class, () -> {
      ResponseEntity any = vehicleEntrantsController
          .vehicleEntrant(vehicleEntrantsDto, "any", UUID.randomUUID().toString());
    });
  }
}