package uk.gov.caz.tariff.controller;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.tariff.dto.CleanAirZone;
import uk.gov.caz.tariff.dto.CleanAirZones;
import uk.gov.caz.tariff.dto.InformationUrls;
import uk.gov.caz.tariff.dto.Rates;
import uk.gov.caz.tariff.dto.Tariff;
import uk.gov.caz.tariff.service.CleanAirZonesRepository;
import uk.gov.caz.tariff.service.TariffRepository;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class})
class CleanAirZonesControllerTest {

  private static final String SOME_CLEAN_AIR_ZONE_ID = "dc1efcaf-a2cf-41ec-aa37-ea4b28a20a1d";

  private static final String SOME_URL = "www.test.uk";

  @Mock
  private TariffRepository tariffRepository;

  @Mock
  private CleanAirZonesRepository cleanAirZonesRepository;

  @InjectMocks
  private CleanAirZonesController cleanAirZonesController;

  @Test
  public void shouldReturnCleanAirZones() {
    // given
    when(cleanAirZonesRepository.findAll()).thenReturn(prepareCleanAirZones());

    // when
    ResponseEntity<CleanAirZones> cleanAirZones = cleanAirZonesController.cleanAirZones();

    // then
    assertThat(cleanAirZones).isNotNull();
    assertThat(cleanAirZones.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(cleanAirZones.getBody().getCleanAirZones()).contains(
        CleanAirZone.builder()
            .cleanAirZoneId(UUID.fromString("0d7ab5c4-5fff-4935-8c4e-56267c0c9493"))
            .name("Birmingham")
            .boundaryUrl(URI.create(
                "https://www.birmingham.gov.uk/info/20076/pollution/1763/a_clean_air_zone_for_birmingham/3"))
            .build()
    );
  }

  @Test
  public void shouldReturnIncomingXCorrelationId() {
    // given

    // when
    ResponseEntity<CleanAirZones> cleanAirZones = cleanAirZonesController.cleanAirZones();

    // then
    assertThat(cleanAirZones).isNotNull();
    assertThat(cleanAirZones.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
  }

  @Test
  public void shouldReturnSomeTariff() {
    // given
    when(tariffRepository.findByCleanAirZoneId(UUID.fromString(SOME_CLEAN_AIR_ZONE_ID)))
        .thenReturn(prepareTariff());

    // when
    ResponseEntity<Tariff> tariff = cleanAirZonesController.tariff(SOME_CLEAN_AIR_ZONE_ID);

    // then
    assertThat(tariff).isNotNull();
    assertThat(tariff.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(tariff.getBody()).isEqualTo(prepareTariff().get());
  }

  @Test
  public void shouldReturnNotFoundForTariff() {
    // given

    // when
    ResponseEntity<Tariff> tariff = cleanAirZonesController.tariff(SOME_CLEAN_AIR_ZONE_ID);

    // then
    assertThat(tariff.getStatusCode()).isEqualByComparingTo(HttpStatus.NOT_FOUND);
  }

  public Optional<Tariff> prepareTariff() {
    InformationUrls informationUrls = InformationUrls.builder()
        .becomeCompliant(SOME_URL)
        .boundary(SOME_URL)
        .exemptionOrDiscount(SOME_URL)
        .hoursOfOperation(SOME_URL)
        .payCaz(SOME_URL)
        .pricing(SOME_URL)
        .mainInfo(SOME_URL)
        .financialAssistance(SOME_URL)
        .build();
    Rates rates = Rates.builder()
        .bus(new BigDecimal("5.50"))
        .coach(new BigDecimal("15.60"))
        .hgv(new BigDecimal("5.69"))
        .largeVan(new BigDecimal("100.00"))
        .moped(new BigDecimal("49.49"))
        .motorcycle(new BigDecimal("80.01"))
        .phv(new BigDecimal("80.10"))
        .car(new BigDecimal("80.10"))
        .miniBus(new BigDecimal("80.10"))
        .smallVan(new BigDecimal("80.00"))
        .taxi(new BigDecimal("2.00"))
        .build();
    Tariff tariff = Tariff.builder()
        .cleanAirZoneId(UUID.fromString(SOME_CLEAN_AIR_ZONE_ID))
        .name("Leeds")
        .tariffClass('C')
        .informationUrls(informationUrls)
        .rates(rates)
        .build();

    return Optional.ofNullable(tariff);
  }

  private CleanAirZones prepareCleanAirZones() {
    return new CleanAirZones(
        newArrayList(
            caz("Birmingham", "0d7ab5c4-5fff-4935-8c4e-56267c0c9493",
                "https://www.birmingham.gov.uk/info/20076/pollution/"
                    + "1763/a_clean_air_zone_for_birmingham/3"),

            caz("Leeds", "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3",
                "https://www.arcgis.com/home/webmap/viewer.html?webmap="
                    + "de0120ae980b473982a3149ab072fdfc&extent=-1.733%2c53.7378%2c-1.333%2c53.8621")
        ));
  }

  private CleanAirZone caz(String cazName, String cleanAirZoneId, String boundaryUrl) {
    return CleanAirZone.builder()
        .name(cazName)
        .cleanAirZoneId(UUID.fromString(cleanAirZoneId))
        .boundaryUrl(URI.create(boundaryUrl))
        .build();
  }
}