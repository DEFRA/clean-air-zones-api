package uk.gov.caz.vcc.repository;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.DvlaVehiclesInformation;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.NtrAndDvlaData;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

class LicenseAndVehicleRemoteRepositoryTest {

  public static final String ANY = "any";

  private static final String VRN1 = "VRN1";
  private static final String VRN2 = "VRN2";
  private static final Integer MAX_DVLA_CONCURRENT_CALLS = 10;
  private static final long TIMEOUT = 20;

  @Mock
  private NationalTaxiRegisterAsyncRepository nationalTaxiRegisterAsyncRepository;

  @Mock
  private VehicleRemoteRepository dvlaRemoteRepository;

  @Mock
  private AsyncRestService asyncRestService;

  @Mock
  private AsyncOp<TaxiPhvLicenseInformationResponse> ntrAsyncOp;

  @Mock
  private AsyncOp<Vehicle> dvlaAsyncOp;

  @Mock
  private VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator;

  private LicenseAndVehicleRemoteRepository licenseAndVehicleRemoteRepository;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);

    this.licenseAndVehicleRemoteRepository = new LicenseAndVehicleRemoteRepository(
        nationalTaxiRegisterAsyncRepository, dvlaRemoteRepository, asyncRestService,
        remoteAuthenticationTokenGenerator, MAX_DVLA_CONCURRENT_CALLS, TIMEOUT);
  }

  @Test
  void shouldCallRemoteNtrApiAndRemoteVehicleApi() {
    // given
    when(nationalTaxiRegisterAsyncRepository.findByRegistrationNumberAsync(anyString()))
        .thenReturn(ntrAsyncOp);
    when(dvlaRemoteRepository.findByRegistrationNumberAsync(anyString(), anyString()))
        .thenReturn(dvlaAsyncOp);

    // when
    NtrAndDvlaData licenseAndVehicle =
        licenseAndVehicleRemoteRepository.findLicenseAndVehicle(ANY, ANY);

    // then
    assertThat(licenseAndVehicle.getNtrLicences()).hasSize(1);
    assertThat(licenseAndVehicle.getDvlaVehicles()).hasSize(1);
    verify(asyncRestService).startAndAwaitAll(anyList(), eq(20L), eq(TimeUnit.SECONDS));
  }

  @Test
  void shouldCallRemoteVehicleApi() {
    // given
    when(dvlaRemoteRepository.findByRegistrationNumberAsync(anyString(), anyString()))
        .thenReturn(dvlaAsyncOp);

    // when
    NtrAndDvlaData licenseAndVehicle = licenseAndVehicleRemoteRepository
        .findVehicles(Collections.singletonList(ANY), ANY);

    // then
    assertThat(licenseAndVehicle.getDvlaVehicles()).hasSize(1);
    verify(asyncRestService).startAndAwaitAll(anyList(), eq(20L), eq(TimeUnit.SECONDS));
  }

  @Nested
  class FindingDvlaVehiclesInBulk {

    @Test
    public void whenAllAsyncOpsSucceeded() {
      // given
      when(remoteAuthenticationTokenGenerator.getAuthenticationToken()).thenReturn(ANY);
      when(dvlaRemoteRepository.findByRegistrationNumberAsync(VRN1, ANY))
          .thenReturn(AsyncOp.asCompletedAndSuccessful(VRN1,
              HttpStatus.OK, new Vehicle()));
      when(dvlaRemoteRepository.findByRegistrationNumberAsync(VRN2, ANY))
          .thenReturn(AsyncOp.asCompletedAndSuccessful(VRN2,
              HttpStatus.OK, new Vehicle()));
      val vrns = newArrayList(VRN1, VRN2);

      // when
      DvlaVehiclesInformation foundDvlaVehicles = licenseAndVehicleRemoteRepository
          .findDvlaVehiclesInBulk(vrns);

      // then
      assertThat(foundDvlaVehicles.hasFailed()).isFalse();
      assertThat(foundDvlaVehicles.getDvlaVehicleInfoFor(VRN1).hasFailed()).isFalse();
      assertThat(foundDvlaVehicles.getDvlaVehicleInfoFor(VRN2).hasFailed()).isFalse();
    }

    @Test
    public void whenAsyncOperationFailed() {
      // given
      when(remoteAuthenticationTokenGenerator.getAuthenticationToken()).thenReturn(ANY);
      AsyncOp<Vehicle> vrn1AsyncOp = AsyncOp.asCompletedAndSuccessful(VRN1,
          HttpStatus.OK, new Vehicle());
      when(dvlaRemoteRepository.findByRegistrationNumberAsync(VRN1, ANY))
          .thenReturn(vrn1AsyncOp);
      AsyncOp<Vehicle> vrn2AsyncOp = AsyncOp.asCompletedAndSuccessful(VRN2,
          HttpStatus.OK, new Vehicle());
      when(dvlaRemoteRepository.findByRegistrationNumberAsync(VRN2, ANY))
          .thenReturn(vrn2AsyncOp);
      doThrow(new RuntimeException()).when(asyncRestService)
          .startAndAwaitAll(newArrayList(vrn1AsyncOp, vrn2AsyncOp), 20, TimeUnit.SECONDS);
      val vrns = newArrayList(VRN1, VRN2);

      // when
      DvlaVehiclesInformation foundDvlaVehicles = licenseAndVehicleRemoteRepository
          .findDvlaVehiclesInBulk(vrns);

      // then
      assertThat(foundDvlaVehicles.hasFailed()).isTrue();
    }
  }
}
