package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

@ExtendWith(MockitoExtension.class)
class NationalTaxiRegisterRepositoryTest {

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  private String nationalTaxiRegisterRootUri = "";

  private NationalTaxiRegisterRepository nationalTaxiRegisterRepository;

  @Mock
  private RestTemplate nationalTaxiRegisterRestTemplate;

  @Mock
  private NationalTaxiRegisterAsyncRepository asyncRepository;

  @Mock
  private AsyncRestService asyncRestService;

  @Mock
  private ResponseEntity responseEntity;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilder.rootUri(anyString())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(nationalTaxiRegisterRestTemplate);
    nationalTaxiRegisterRepository = new NationalTaxiRegisterRepository(restTemplateBuilder,
        nationalTaxiRegisterRootUri, asyncRepository, asyncRestService);
  }

  @Test
  void shouldReturnNotEmptyBody() {
    //given
    String vrn = "any vrn";

    when(nationalTaxiRegisterRestTemplate.getForEntity(anyString(), any(), anyString()))
        .thenReturn(responseEntity);
    when(responseEntity.getBody()).thenReturn(TaxiPhvLicenseInformationResponse.builder().build());

    //when
    Optional<TaxiPhvLicenseInformationResponse> licenseInfoResponse = nationalTaxiRegisterRepository
        .getLicenseInfo(vrn);
    //then
    assertThat(licenseInfoResponse).isPresent();
  }

  @Test
  void shouldReturnEmptyBody() {
    //given
    String vrn = "any vrn";

    when(nationalTaxiRegisterRestTemplate.getForEntity(anyString(), any(), anyString()))
        .thenReturn(responseEntity);

    //when
    Optional<TaxiPhvLicenseInformationResponse> licenseInfoResponse = nationalTaxiRegisterRepository
        .getLicenseInfo(vrn);
    //then
    assertThat(licenseInfoResponse).isNotPresent();
  }

  @Test
  void shouldThrowExternalCallServiceExceptionIfAnyRuntimeExceptionWasThrownDuringCallToNTR() {
    //given
    String vrn = "any vrn";

    when(nationalTaxiRegisterRestTemplate.getForEntity(anyString(), any(), anyString()))
        .thenThrow(new RuntimeException());

    //then
    assertThrows(ExternalServiceCallException.class,
        () -> nationalTaxiRegisterRepository.getLicenseInfo(vrn));
  }
}