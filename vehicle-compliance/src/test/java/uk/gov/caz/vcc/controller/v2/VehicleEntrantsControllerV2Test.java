package uk.gov.caz.vcc.controller.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.caz.vcc.controller.v2.VehicleEntrantsControllerV2.TimestampHeaderValidationException;
import uk.gov.caz.vcc.controller.v2.VehicleEntrantsControllerV2.VehicleEntrantsListNullException;
import uk.gov.caz.vcc.dto.ErrorsResponse;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsDtoV2;
import uk.gov.caz.vcc.dto.validation.ValidationError;
import uk.gov.caz.vcc.service.VehicleEntrantsService;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantsControllerV2Test {

  @Mock
  private VehicleEntrantsService vehicleEntrantsService;

  private VehicleEntrantsControllerV2 vehicleEntrantsControllerV2;

  private static final int MAX_ERRORS_COUNT = 5;

  private static final String ANY_CORRELATION_ID = "ed6bda66-7a94-43bd-ba64-30b668281a7f";

  private static final String ANY_CAZ_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1b";

  private static final String VALID_TIMESTAMP = "2019-03-01T13:00:00Z";

  private static final String VALID_VRN = "CAS256";

  @BeforeEach
  public void setUp() {
    vehicleEntrantsControllerV2 = new VehicleEntrantsControllerV2(vehicleEntrantsService,
        MAX_ERRORS_COUNT);
  }

  @Test
  public void shouldReturnStatus200OnValidRequest() {
    // given
    VehicleEntrantsDtoV2 request = createRequest(VALID_VRN, VALID_TIMESTAMP);

    // when
    ResponseEntity response = vehicleEntrantsControllerV2
        .vehicleEntrants(request, ANY_CORRELATION_ID, ANY_CAZ_ID, VALID_TIMESTAMP);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void shouldReturnStatus400WhenVrnValidationFails() {
    // given
    VehicleEntrantsDtoV2 request = createRequest(null, VALID_TIMESTAMP);

    // when
    ResponseEntity response = vehicleEntrantsControllerV2
        .vehicleEntrants(request, ANY_CORRELATION_ID, ANY_CAZ_ID, VALID_TIMESTAMP);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnStatus400WhenTimestampInBodyValidationFails() {
    // given
    VehicleEntrantsDtoV2 request = createRequest(VALID_VRN, null);

    // when
    ResponseEntity response = vehicleEntrantsControllerV2
        .vehicleEntrants(request, ANY_CORRELATION_ID, ANY_CAZ_ID, VALID_TIMESTAMP);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnErrorWhenVehicleEntrantsAreMissing() {
    // given
    VehicleEntrantsDtoV2 request = createEmptyRequest();

    // when
    ResponseEntity response = vehicleEntrantsControllerV2
        .vehicleEntrants(request, ANY_CORRELATION_ID, ANY_CAZ_ID, VALID_TIMESTAMP);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorsResponse castErrors = (ErrorsResponse) response.getBody();
    assertThat(castErrors.getErrors().size()).isEqualTo(1);
    assertThat(castErrors.getErrors().get(0).getTitle()).isEqualTo("Vehicle Entrants not present");
    assertThat(castErrors.getErrors().get(0).getDetail()).isEqualTo("List of vehicle entrants cannot not be null.");
    assertThat(castErrors.getErrors().get(0).getVrn()).isNull();
    assertThat(castErrors.getErrors().get(0).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  public void shouldReturnStatus400WhenTimestampInHeaderValidationFails() {
    // given
    VehicleEntrantsDtoV2 request = createRequest(VALID_VRN, VALID_TIMESTAMP);
    String invalidTimestamp = "invalid-timestamp";

    // when
    ResponseEntity response = vehicleEntrantsControllerV2
        .vehicleEntrants(request, ANY_CORRELATION_ID, ANY_CAZ_ID, invalidTimestamp);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorsResponse castErrors = (ErrorsResponse) response.getBody();
    assertThat(castErrors.getErrors().size()).isEqualTo(1);
    assertThat(castErrors.getErrors().get(0).getTitle()).isEqualTo("Invalid Timestamp request header value");
    assertThat(castErrors.getErrors().get(0).getDetail()).isEqualTo(String.format(
            "Invalid Timestamp header for value: %s. The expected format is 2020-05-22T13:26:00Z",
            invalidTimestamp));
    assertThat(castErrors.getErrors().get(0).getVrn()).isNull();
    assertThat(castErrors.getErrors().get(0).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  private VehicleEntrantsDtoV2 createRequest(String vrn, String timestamp) {
    VehicleEntrantDto vehicleEntrant = VehicleEntrantDto.builder()
        .vrn(vrn)
        .timestamp(timestamp)
        .build();

    return VehicleEntrantsDtoV2.builder()
        .vehicleEntrants(Collections.singletonList(vehicleEntrant))
        .build();
  }

  private VehicleEntrantsDtoV2 createEmptyRequest() {
    return VehicleEntrantsDtoV2.builder().build();
  }
}