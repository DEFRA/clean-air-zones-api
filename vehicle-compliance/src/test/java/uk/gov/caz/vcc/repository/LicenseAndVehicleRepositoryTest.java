package uk.gov.caz.vcc.repository;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.NtrAndDvlaData;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.NtrAndDvlaData.Data;

class LicenseAndVehicleRepositoryTest {

  private static final String VRN1 = "VRN1";

  @Test
  public void testWhenAllIsOk() {
    // given
    NtrAndDvlaData ntrAndDvlaData = NtrAndDvlaData.builder()
        .ntrLicence(VRN1,
            AsyncOp.asCompletedAndSuccessful(VRN1, HttpStatus.OK, licenseInformationResponse())).
            dvlaVehicle(VRN1, AsyncOp.asCompletedAndSuccessful(VRN1, HttpStatus.OK, new Vehicle())).
            build();

    // when
    Data<TaxiPhvLicenseInformationResponse> licenceForVrn1 = ntrAndDvlaData
        .ntrFor(VRN1);
    Data<Vehicle> vehicleDataForVrn1 = ntrAndDvlaData.dvlaFor(VRN1);

    // then
    assertThat(licenceForVrn1.hasError()).isFalse();
    assertThat(licenceForVrn1.getHttpStatus()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(vehicleDataForVrn1.hasError()).isFalse();
    assertThat(vehicleDataForVrn1.getHttpStatus()).isEqualByComparingTo(HttpStatus.OK);
  }

  @Test
  public void testWhenNtrAndDvlaHaveErrors() {
    // given
    NtrAndDvlaData ntrAndDvlaData = NtrAndDvlaData.builder()
        .ntrLicence(VRN1,
            AsyncOp.asCompletedAndFailed(VRN1, HttpStatus.NOT_FOUND, "NOT FOUND")).
            dvlaVehicle(VRN1, AsyncOp
                .asCompletedAndFailed(VRN1, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE UNAVAILABLE")).
            build();

    // when
    Data<TaxiPhvLicenseInformationResponse> licenceForVrn1 = ntrAndDvlaData
        .ntrFor(VRN1);
    Data<Vehicle> vehicleDataForVrn1 = ntrAndDvlaData.dvlaFor(VRN1);

    // then
    assertThat(licenceForVrn1.hasError()).isTrue();
    assertThat(licenceForVrn1.getHttpStatus()).isEqualByComparingTo(HttpStatus.NOT_FOUND);
    assertThat(vehicleDataForVrn1.hasError()).isTrue();
    assertThat(vehicleDataForVrn1.getHttpStatus())
        .isEqualByComparingTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  private TaxiPhvLicenseInformationResponse licenseInformationResponse() {
    return TaxiPhvLicenseInformationResponse.builder().active(true).build();
  }
}