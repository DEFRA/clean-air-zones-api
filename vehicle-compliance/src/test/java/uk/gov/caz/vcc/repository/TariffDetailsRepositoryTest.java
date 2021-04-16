package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.definitions.dto.InformationUrlsDto;
import uk.gov.caz.definitions.dto.RatesDto;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.CazClass;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.dto.TariffDto;

@ExtendWith(MockitoExtension.class)
public class TariffDetailsRepositoryTest {

  public static final HttpClientErrorException NOT_FOUND_EXCEPTION = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[]{1},
      Charset.defaultCharset());

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate restTemplate;

  private TariffDetailsRepository tariffDetailsRepository;

  @BeforeEach
  void setup() {
    when(restTemplateBuilder.rootUri(Mockito.anyString()))
        .thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    tariffDetailsRepository = new TariffDetailsRepository(restTemplateBuilder, "test");
  }

  @Test
  void canMapExternalTariffToInternalDomainObject() {

    UUID testCleanAirZoneId = UUID.randomUUID();
    InformationUrlsDto testUrls = InformationUrlsDto.builder()
        .becomeCompliant("become-compliant").boundary("boundary")
        .exemptionOrDiscount("exemptions")
        .mainInfo("main-info")
        .build();

    RatesDto testRates = RatesDto.builder().bus(new BigDecimal("10.1"))
        .car(new BigDecimal("10.2")).coach(new BigDecimal("10.3"))
        .hgv(new BigDecimal("10.4")).van(new BigDecimal("10.5"))
        .motorcycle(new BigDecimal("10.6")).miniBus(new BigDecimal("10.8"))
        .van(new BigDecimal("10.9")).taxi(new BigDecimal("10.10")).build();

    TariffDto testExternalTariff = TariffDto.builder()
        .cleanAirZoneId(testCleanAirZoneId).tariffClass('A')
        .informationUrls(testUrls).rates(testRates).build();

    TariffDetails internalRepresentation = TariffDetailsRepository
        .mapFromTariffDto(testExternalTariff);

    assertThat(internalRepresentation).isNotNull();
    assertThat(internalRepresentation.getCazId())
        .isEqualTo(testExternalTariff.getCleanAirZoneId());
    assertThat(internalRepresentation.getCazClass())
        .isEqualTo(CazClass.fromChar(testExternalTariff.getTariffClass()));
    assertThat(internalRepresentation.getInformationUrls())
        .isEqualTo(testExternalTariff.getInformationUrls());
    assertThat(internalRepresentation.isChargesMotorcycles())
        .isEqualTo(testExternalTariff.isMotorcyclesChargeable());

  }

  @Test
  void shouldThrowExternalServiceCallExceptionIfAnyRuntimeWasThrownDuringFetchingCAZes() {
    //when
    when(restTemplate.getForEntity(anyString(), any())).thenThrow(new RuntimeException());

    //then
    assertThrows(ExternalServiceCallException.class,
        () -> tariffDetailsRepository.getCleanAirZoneListing());
  }

  @Test
  void shouldThrowExternalServiceCallExceptionIfAnyRuntimeWasThrownDuringFetchingTariffs() {
    //when
    when(restTemplate.getForEntity(anyString(), any(), any(UUID.class)))
        .thenThrow(new RuntimeException());

    //then
    assertThrows(ExternalServiceCallException.class,
        () -> tariffDetailsRepository.getTariffDetails(UUID.randomUUID()));
  }

  @Test
  void shouldReturnEmptyOptionalIfTariffDetailsForGivenCazNotFound() {
    //given
    UUID cazId = UUID.randomUUID();

    //when
    when(restTemplate.getForEntity("/v1/clean-air-zones/{cazIdentifer}/tariff", TariffDto.class, cazId))
        .thenThrow(NOT_FOUND_EXCEPTION);

    //then
    assertThat(tariffDetailsRepository.getTariffDetails(cazId)).isEmpty();
  }
}
