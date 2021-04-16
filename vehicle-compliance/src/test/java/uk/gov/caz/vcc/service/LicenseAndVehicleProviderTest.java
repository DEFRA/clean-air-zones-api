package uk.gov.caz.vcc.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.DvlaVehiclesInformation;
import uk.gov.caz.vcc.dto.LicencesInformation;
import uk.gov.caz.vcc.dto.NtrAndDvlaVehicleData;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.dto.SingleLicenceData;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository;

@ExtendWith(MockitoExtension.class)
class LicenseAndVehicleProviderTest {

  @Mock
  private LicenseAndVehicleRepository licenseAndVehicleRepository;

  @Mock
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @InjectMocks
  LicenseAndVehicleProvider licenseAndVehicleProvider;

  @Nested
  class WhenPrefetched {

    @Test
    public void shouldGetLicencesAndDvlaInformation() {
      // given
      List<String> vrns = Arrays.asList("AB123", "BC123");
      mockSuccessfulBulkGetLicenceCall(vrns);
      mockSuccessfulBulkGetDvlaVehiclesCall(vrns);

      // when
      licenseAndVehicleProvider.prefetch(vrns);

      // then
      verify(nationalTaxiRegisterService).getLicensesInformation(vrns);
      verify(licenseAndVehicleRepository).findDvlaVehiclesInBulk(vrns);
    }

    private void mockSuccessfulBulkGetLicenceCall(List<String> vrns) {
      given(nationalTaxiRegisterService.getLicensesInformation(vrns)).willReturn(
          LicencesInformation.success(Collections.emptyMap()));
    }

    private void mockSuccessfulBulkGetDvlaVehiclesCall(List<String> vrns) {
      given(licenseAndVehicleRepository.findDvlaVehiclesInBulk(vrns)).willReturn(
          DvlaVehiclesInformation.success(Collections.emptyMap()));
    }
  }

  @Nested
  class FindLicenceAndVehicle {

    @Test
    public void shouldGetCachedNtrAndDvlaData() {
      // given
      List<String> vrns = Arrays.asList("AB123", "BC123");
      mockSuccessfulBulkGetLicenceCall(vrns);
      mockSuccessfulBulkGetDvlaVehiclesCall(vrns);
      licenseAndVehicleProvider.prefetch(vrns);

      // when
      NtrAndDvlaVehicleData result = licenseAndVehicleProvider.findLicenseAndVehicle("AB123");

      // then
      assertThat(result.getNtrVehicleData().hasFailed()).isFalse();
      assertThat(result.getNtrVehicleData())
          .extracting(SingleLicenceData::getLicence)
          .asInstanceOf(optional(TaxiPhvLicenseInformationResponse.class))
          .isNotEmpty();

      assertThat(result.getDvlaVehicleData().hasFailed()).isFalse();
      assertThat(result.getDvlaVehicleData())
          .extracting(SingleDvlaVehicleData::getVehicle)
          .asInstanceOf(type(Vehicle.class))
          .isNotNull();
    }

    @Nested
    class WhenNtrCallFails {

      @Test
      public void shouldReturnDataWithFailureInfo() {
        // given
        String vrn = "AB123";
        mockFailedNtrAndSuccessfulDvlaCalls(singletonList(vrn));
        licenseAndVehicleProvider.prefetch(singletonList(vrn));

        // when
        NtrAndDvlaVehicleData result = licenseAndVehicleProvider.findLicenseAndVehicle(vrn);

        // then
        assertThat(result.getNtrVehicleData().hasFailed()).isTrue();
        assertThat(result.getNtrVehicleData())
            .extracting(SingleLicenceData::getLicence)
            .asInstanceOf(optional(TaxiPhvLicenseInformationResponse.class))
            .isEmpty();
        assertThat(result.getDvlaVehicleData().hasFailed()).isFalse();
        assertThat(result.getDvlaVehicleData()).extracting(SingleDvlaVehicleData::getVehicle)
            .asInstanceOf(type(Vehicle.class)).isNotNull();
      }
    }

    @Nested
    class WhenDvlaCallFails {

      @Test
      public void shouldReturnDataWithFailureInfo() {
        // given
        String vrn = "AB123";
        mockFailedDvlaAndSuccessfulNtrCalls(singletonList(vrn));
        licenseAndVehicleProvider.prefetch(singletonList(vrn));

        // when
        NtrAndDvlaVehicleData result = licenseAndVehicleProvider.findLicenseAndVehicle(vrn);

        // then
        assertThat(result.getNtrVehicleData().hasFailed()).isFalse();
        assertThat(result.getNtrVehicleData())
            .extracting(SingleLicenceData::getLicence)
            .asInstanceOf(optional(TaxiPhvLicenseInformationResponse.class))
            .isNotEmpty();
        assertThat(result.getDvlaVehicleData().hasFailed()).isTrue();
        assertThat(result.getDvlaVehicleData().getVehicle()).isNull();
      }
    }

    @Nested
    class WhenForgotToPrefetch {

      @Test
      public void shouldThrowIfTryingToGetDataWithoutPrefetchingFirst() {
        // given
        String vrn = "AB123";
        // No prefetch call

        // when
        Throwable throwable = catchThrowable(
            () -> licenseAndVehicleProvider.findLicenseAndVehicle(vrn));

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
      }
    }

    private void mockFailedNtrAndSuccessfulDvlaCalls(List<String> vrns) {
      mockSuccessfulBulkGetDvlaVehiclesCall(vrns);
      mockFailedBulkGetLicenceCall(vrns);
    }

    private void mockFailedDvlaAndSuccessfulNtrCalls(List<String> vrns) {
      mockSuccessfulBulkGetLicenceCall(vrns);
      mockFailedBulkGetDvlaVehiclesCall(vrns);
    }

    private void mockSuccessfulBulkGetLicenceCall(List<String> vrns) {
      Map<String, TaxiPhvLicenseInformationResponse> result = vrns.stream()
          .collect(Collectors.toMap(Function.identity(), vrn -> anyLicense()));
      given(nationalTaxiRegisterService.getLicensesInformation(vrns)).willReturn(
          LicencesInformation.success(result));
    }

    private void mockSuccessfulBulkGetDvlaVehiclesCall(List<String> vrns) {
      Map<String, SingleDvlaVehicleData> result = vrns.stream()
          .collect(Collectors.toMap(Function.identity(), vrn -> anyVehicle()));
      given(licenseAndVehicleRepository.findDvlaVehiclesInBulk(vrns)).willReturn(
          DvlaVehiclesInformation.success(result));
    }

    private void mockFailedBulkGetLicenceCall(List<String> vrns) {
      given(nationalTaxiRegisterService.getLicensesInformation(vrns)).willReturn(
          LicencesInformation.failure(HttpStatus.INTERNAL_SERVER_ERROR, "halp!"));
    }

    private void mockFailedBulkGetDvlaVehiclesCall(List<String> vrns) {
      given(licenseAndVehicleRepository.findDvlaVehiclesInBulk(vrns)).willReturn(
          DvlaVehiclesInformation.failure());
    }

    private TaxiPhvLicenseInformationResponse anyLicense() {
      return TaxiPhvLicenseInformationResponse.builder()
          .active(true).build();
    }

    private SingleDvlaVehicleData anyVehicle() {
      return SingleDvlaVehicleData.success(new Vehicle());
    }
  }
}