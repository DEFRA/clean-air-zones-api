package uk.gov.caz.taxiregister.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.taxiregister.model.VrmSet;

class VehicleComplianceCheckerRepositoryTest {

  private static final String ROOT_URI = "https://vccs.gov.uk";

  private RestTemplate mockedRestTemplate;
  private VehicleComplianceCheckerRepository vehicleComplianceCheckerRepository;

  @BeforeEach
  void setup() {
    RestTemplateBuilder mockedRestTemplateBuilder = mock(RestTemplateBuilder.class);
    mockedRestTemplate = mock(RestTemplate.class);

    given(mockedRestTemplateBuilder.rootUri(ROOT_URI)).willReturn(mockedRestTemplateBuilder);
    given(mockedRestTemplateBuilder.build()).willReturn(mockedRestTemplate);
    vehicleComplianceCheckerRepository = new VehicleComplianceCheckerRepository(
        mockedRestTemplateBuilder, ROOT_URI);
  }

  @Test
  void purgeCacheOfNtrDataDelegatesCallToRestTemplate() {
    // given
    VrmSet vrmSet = VrmSet.from(Sets.newHashSet("A1", "B2"));
    given(mockedRestTemplate
        .postForEntity(anyString(), any(HttpEntity.class), any()))
        .willReturn(ResponseEntity.ok(null));

    // when
    vehicleComplianceCheckerRepository.purgeCacheOfNtrData(vrmSet);

    // then
    ArgumentCaptor<HttpEntity<VrmSet>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(mockedRestTemplate)
        .postForEntity(eq(VehicleComplianceCheckerRepository.VCCS_CACHE_INVALIDATION_URL_PATH),
            requestCaptor.capture(),
            eq(Void.class));
    HttpEntity<VrmSet> capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getBody()).isEqualTo(vrmSet);
  }

  @Test
  void shouldSkipPurgeCacheIfEmptyVrms() {
    // given
    VrmSet vrmSet = VrmSet.from(Collections.emptySet());

    // when
    vehicleComplianceCheckerRepository.purgeCacheOfNtrData(vrmSet);

    // then
    verify(mockedRestTemplate,never()).postForEntity(any(),any(), any());
  }
}