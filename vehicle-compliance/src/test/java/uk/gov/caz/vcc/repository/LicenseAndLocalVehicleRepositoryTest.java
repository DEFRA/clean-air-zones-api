package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.async.rest.AsyncResponse;
import uk.gov.caz.async.rest.AsyncResponses;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.LicenseAndVehicleResponse;

@ExtendWith(MockitoExtension.class)
class LicenseAndLocalVehicleRepositoryTest {

  public static final String ANY = "any";
  @Mock
  private NtrRemoteRepository ntrRemoteRepository;
  @Mock
  private LocalVehicleDetailsRepository localVehicleDetailsRepository;
  @Mock
  private AsyncRestService asyncRestService;

  @InjectMocks
  private LicenseAndLocalVehicleRepository licenseAndLocalVehicleRepository;

  @Test
  void shouldCallRemoteNtrApiAndLocalVehicleRepository() {
    //given
    when(asyncRestService.call(anyList())).thenReturn(createResponses());
    when(localVehicleDetailsRepository.findByRegistrationNumber(anyString()))
        .thenReturn(createVehicle());
    //when
    LicenseAndVehicleResponse licenseAndVehicle = licenseAndLocalVehicleRepository
        .findLicenseAndVehicle(ANY, ANY, ANY);
    //then
    assertThat(licenseAndVehicle.getLicensInfos()).hasSize(1);
    assertThat(licenseAndVehicle.getVehicles()).hasSize(1);
  }

  private Optional<Vehicle> createVehicle() {
    return Optional.of(new Vehicle());
  }

  private AsyncResponses createResponses() {
    HashMap<String, AsyncResponse<Object>> map = new HashMap<>();
    map.put(ANY, AsyncResponse.success(new Object()));
    return new AsyncResponses<>(map, new CountDownLatch(0));
  }
}