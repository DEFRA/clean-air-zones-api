package uk.gov.caz.taxiregister.service;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import uk.gov.caz.taxiregister.model.VrmSet;
import uk.gov.caz.taxiregister.repository.VehicleComplianceCheckerRepository;

class VehicleComplianceCheckerServiceTest {

  @Test
  void whenDisabledPurgeDoesNothing() {
    // given
    VehicleComplianceCheckerRepository mockedRepository = mock(
        VehicleComplianceCheckerRepository.class);
    VehicleComplianceCheckerService service = new VehicleComplianceCheckerService(mockedRepository,
        false);
    VrmSet vrmSet = VrmSet.from(Sets.newHashSet("A1", "B2"));

    // when
    service.purgeCacheOfNtrData(vrmSet);

    // then
    verifyZeroInteractions(mockedRepository);
  }

  @Test
  void whenEnabledPurgeDelegatesToRepository() {
    // given
    VehicleComplianceCheckerRepository mockedRepository = mock(
        VehicleComplianceCheckerRepository.class);
    VehicleComplianceCheckerService service = new VehicleComplianceCheckerService(mockedRepository,
        true);
    VrmSet vrmSet = VrmSet.from(Sets.newHashSet("A1", "B2"));

    // when
    service.purgeCacheOfNtrData(vrmSet);

    // then
    verify(mockedRepository).purgeCacheOfNtrData(vrmSet);
  }

  @Test
  void anyRestClientExceptionMakesGracefulReturn() {
    // given
    VrmSet vrmSet = VrmSet.from(Sets.newHashSet("A1", "B2"));
    VehicleComplianceCheckerRepository mockedRepository = mock(
        VehicleComplianceCheckerRepository.class);
    willThrow(new RestClientException("Exception")).given(mockedRepository)
        .purgeCacheOfNtrData(vrmSet);
    VehicleComplianceCheckerService service = new VehicleComplianceCheckerService(mockedRepository,
        true);

    // when
    service.purgeCacheOfNtrData(vrmSet);

    // then
    // no exception thrown, so we are here
    verify(mockedRepository).purgeCacheOfNtrData(vrmSet);
  }
}