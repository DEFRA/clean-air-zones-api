package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.NtrAndDvlaData;

@ExtendWith(MockitoExtension.class)
class LicenseAndLocalVehicleRemoteAsyncRepositoryTest {

  public static final String ANY = "any";
  @Mock
  private NationalTaxiRegisterAsyncRepository nationalTaxiRegisterAsyncRepository;
  @Mock
  private LocalVehicleDetailsRepository localVehicleDetailsRepository;
  @Mock
  private AsyncRestService asyncRestService;
  @Mock
  private AsyncOp<TaxiPhvLicenseInformationResponse> asyncOp;

  @InjectMocks
  private LicenseAndVehicleLocalRepository licenseAndVehicleLocalRepository;

  @Test
  void shouldCallRemoteNtrApiAndLocalVehicleRepository() {
    // given
    when(nationalTaxiRegisterAsyncRepository.findByRegistrationNumberAsync(anyString()))
        .thenReturn(asyncOp);
    when(localVehicleDetailsRepository.findByRegistrationNumber(anyString()))
        .thenReturn(createVehicle());

    // when
    NtrAndDvlaData licenseAndVehicle = licenseAndVehicleLocalRepository
        .findLicenseAndVehicle(ANY, ANY);

    // then
    assertThat(licenseAndVehicle.getNtrLicences()).hasSize(1);
    assertThat(licenseAndVehicle.getDvlaVehicles()).hasSize(1);
    verify(asyncRestService).startAndAwaitAll(anyList(), eq(20L), eq(TimeUnit.SECONDS));
  }

  @Test
  void shouldCallLocalVehicleRepository() {
    // given
    when(localVehicleDetailsRepository.findByRegistrationNumber(anyString()))
        .thenReturn(createVehicle());

    // when
    NtrAndDvlaData licenseAndVehicle = licenseAndVehicleLocalRepository
        .findVehicles(Collections.singletonList(ANY), ANY);

    // then
    assertThat(licenseAndVehicle.getDvlaVehicles()).hasSize(1);
    verifyNoInteractions(asyncRestService);
  }

  private Optional<Vehicle> createVehicle() {
    return Optional.of(new Vehicle());
  }
}