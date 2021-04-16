package uk.gov.caz.tariff.controller;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.tariff.dto.InformationUrls;
import uk.gov.caz.tariff.dto.Rates;
import uk.gov.caz.tariff.dto.Tariff;
import uk.gov.caz.tariff.service.CleanAirZonesRepository;
import uk.gov.caz.tariff.service.TariffRepository;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class})
class CleanAirZonesControllerTest {

  private static final String SOME_CLEAN_AIR_ZONE_ID = "dc1efcaf-a2cf-41ec-aa37-ea4b28a20a1d";
  private static LocalDate ACTIVE_CHARGE_START_DATE = LocalDate.of(2018, 10, 28);
  private static final String SOME_URL = "www.test.uk";
  private static final String MAIN_INFO_URL = "www.main.info";

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
    ResponseEntity<CleanAirZonesDto> cleanAirZones = cleanAirZonesController.cleanAirZones();

    // then
    assertThat(cleanAirZones).isNotNull();
    assertThat(cleanAirZones.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(cleanAirZones.getBody().getCleanAirZones()).contains(
        CleanAirZoneDto.builder()
            .cleanAirZoneId(UUID.fromString("0d7ab5c4-5fff-4935-8c4e-56267c0c9493"))
            .name("Birmingham")
            .boundaryUrl(URI.create(
                "https://www.birmingham.gov.uk/info/20076/pollution/1763/a_clean_air_zone_for_birmingham/3"))
            .exemptionUrl(URI.create("https://exemption.birmingham.gov.uk"))
            .activeChargeStartDate("2018-10-28")
            .mainInfoUrl(URI.create(MAIN_INFO_URL))
            .build()
    );
  }

  @Test
  public void shouldReturnIncomingXCorrelationId() {
    // given

    // when
    ResponseEntity<CleanAirZonesDto> cleanAirZones = cleanAirZonesController.cleanAirZones();

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
        .mainInfo(SOME_URL)
        .build();
    Rates rates = Rates.builder()
        .bus(new BigDecimal("5.50"))
        .coach(new BigDecimal("15.60"))
        .hgv(new BigDecimal("5.69"))
        .moped(new BigDecimal("49.49"))
        .motorcycle(new BigDecimal("80.01"))
        .phv(new BigDecimal("80.10"))
        .car(new BigDecimal("80.10"))
        .miniBus(new BigDecimal("80.10"))
        .van(new BigDecimal("80.00"))
        .taxi(new BigDecimal("2.00"))
        .build();
    Tariff tariff = Tariff.builder()
        .cleanAirZoneId(UUID.fromString(SOME_CLEAN_AIR_ZONE_ID))
        .name("Bath")
        .tariffClass('C')
        .informationUrls(informationUrls)
        .rates(rates)
        .activeChargeStartDate("2018-10-28")
        .build();

    return Optional.ofNullable(tariff);
  }

  private CleanAirZonesDto prepareCleanAirZones() {
    return CleanAirZonesDto.builder().cleanAirZones(
        newArrayList(
            caz("Birmingham", "0d7ab5c4-5fff-4935-8c4e-56267c0c9493",
                "https://www.birmingham.gov.uk/info/20076/pollution/"
                    + "1763/a_clean_air_zone_for_birmingham/3",
                "https://exemption.birmingham.gov.uk",
                MAIN_INFO_URL,
                ACTIVE_CHARGE_START_DATE),

            caz("Bath", "5dd5c926-ed33-4a0a-b911-46324433e866",
                "http://www.bathnes.gov.uk/zonemaps",
                "http://www.bathnes.gov.uk/CAZexemptions",
                MAIN_INFO_URL,
                ACTIVE_CHARGE_START_DATE)
        )).build();
  }

  private CleanAirZoneDto caz(String cazName, String cleanAirZoneId, String boundaryUrl,
      String exemptionUrl, String mainInfoUrl, LocalDate activeChargeStartDate) {
    return CleanAirZoneDto.builder()
        .name(cazName)
        .cleanAirZoneId(UUID.fromString(cleanAirZoneId))
        .boundaryUrl(URI.create(boundaryUrl))
        .exemptionUrl(URI.create(exemptionUrl))
        .mainInfoUrl(URI.create(mainInfoUrl))
        .activeChargeStartDate(activeChargeStartDate.format(DateTimeFormatter.ISO_DATE))
        .build();
  }
}