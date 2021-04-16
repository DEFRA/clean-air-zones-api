package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
class HttpBasedModRepositoryTestIT extends MockServerTestIT {

  private static final String VRN = "CAS312";

  @Autowired
  private HttpBasedModRepository httpBasedModRepository;

  @AfterEach
  public void clear() {
    mockServer.reset();
  }

  @Test
  public void shouldCheckThatModVehicleExists() {
    //given
    whenModVehicleIsInModService();

    //when
    Boolean vehicleExists = httpBasedModRepository.existsByVrnIgnoreCase(VRN);

    //then
    assertThat(vehicleExists).isTrue();
  }

  @Test
  public void shouldCheckThatModVehicleDoesntExists() {
    //when
    Boolean vehicleExists = httpBasedModRepository.existsByVrnIgnoreCase(VRN);

    //then
    assertThat(vehicleExists).isFalse();
  }

  @Test
  public void shouldFetchModVehicle() {
    //given
    whenModVehicleIsInModService();
    MilitaryVehicle expectedMilitaryVehicle = new MilitaryVehicle();
    expectedMilitaryVehicle.setVrn(VRN);
    expectedMilitaryVehicle.setModWhitelistType("GREEN VEHICLE");
    expectedMilitaryVehicle.setWhitelistDiscountCode("WDC001");

    //when
    MilitaryVehicle militaryVehicle = httpBasedModRepository.findByVrnIgnoreCase(VRN);

    //then
    assertThat(militaryVehicle.getVrn()).isEqualTo(expectedMilitaryVehicle.getVrn());
    assertThat(militaryVehicle.getWhitelistDiscountCode())
        .isEqualTo(expectedMilitaryVehicle.getWhitelistDiscountCode());
  }

  @Test
  public void shouldReturnNullIfVehicleDoesntExist() {
    //given
    whenModVehicleReturns(404);

    //when
    MilitaryVehicle militaryVehicle = httpBasedModRepository.findByVrnIgnoreCase(VRN);

    //then
    assertThat(militaryVehicle).isNull();
  }

  @Test
  public void shouldThrowAnExceptionIfUnknownHttpCodeWasReturned() {
    //given
    whenModVehicleReturns(500);

    //then
    assertThrows(ApplicationRuntimeException.class, () -> {
      httpBasedModRepository.findByVrnIgnoreCase(VRN);
    });
  }

  private void whenModVehicleIsInModService() {
    mockServer.when(requestGet("/v1/mod/" + HttpBasedModRepositoryTestIT.VRN))
        .respond(response("mod-vehicle-response.json"));
  }

  private void whenModVehicleReturns(int status) {
    mockServer.when(requestGet("/v1/mod/" + HttpBasedModRepositoryTestIT.VRN))
        .respond(HttpResponse.response()
            .withStatusCode(status)
            .withHeaders(new Header("Content-Type", "application/json; charset=utf-8")));
  }
}