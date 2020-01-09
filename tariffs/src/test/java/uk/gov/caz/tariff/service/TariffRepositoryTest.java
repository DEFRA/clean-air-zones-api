package uk.gov.caz.tariff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.tariff.dto.InformationUrls;
import uk.gov.caz.tariff.dto.Rates;
import uk.gov.caz.tariff.dto.Tariff;
import uk.gov.caz.tariff.service.TariffRepository.TariffRowMapper;

@ExtendWith(MockitoExtension.class)
class TariffRepositoryTest {

  private static final UUID SOME_CLEAN_AIR_ZONE_ID = UUID
      .fromString("dc1efcaf-a2cf-41ec-aa37-ea4b28a20a1d");

  private static final String SOME_URL = "www.test.uk";

  private static final String SOME_CHARGE_IDENTIFIER = "LCC01";

  private static final String LEEDS = "Leeds";

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private TariffRepository tariffRepository;

  @Test
  public void shouldFindSomeTariff() {
    // given
    Tariff tariff = mockTariffInDB();

    // when
    Optional<Tariff> result = tariffRepository.findByCleanAirZoneId(SOME_CLEAN_AIR_ZONE_ID);

    // then
    assertThat(result)
        .isPresent()
        .contains(tariff);
  }

  @Test
  public void shouldNotFindAnyTariff() {
    // given

    // when
    Optional<Tariff> result = tariffRepository.findByCleanAirZoneId(SOME_CLEAN_AIR_ZONE_ID);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void shouldReturnEmptyOptionalWhenThrowException() {
    // given
    when(jdbcTemplate.queryForObject(anyString(), any(TariffRowMapper.class), any()))
        .thenThrow(EmptyResultDataAccessException.class);

    // when
    Optional<Tariff> result = tariffRepository.findByCleanAirZoneId(SOME_CLEAN_AIR_ZONE_ID);

    // then
    assertThat(result).isEmpty();
  }

  @Nested
  class RowMapper {

    private TariffRowMapper rowMapper = new TariffRowMapper();

    @Test
    public void shouldMapResultSetToTariff() throws SQLException {
      ResultSet resultSet = mockResultSet();

      Tariff tariff = rowMapper.mapRow(resultSet, 0);

      assertThat(tariff).isEqualToComparingFieldByFieldRecursively(expectedTariff());
    }

    private ResultSet mockResultSet() throws SQLException {
      ResultSet resultSet = mock(ResultSet.class);
      when(resultSet.getObject("clean_air_zone_id", UUID.class)).thenReturn(SOME_CLEAN_AIR_ZONE_ID);

      when(resultSet.getString(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case "caz_name":
            return LEEDS;
          case "charge_identifier":
            return SOME_CHARGE_IDENTIFIER;
          case "caz_class":
            return String.valueOf('C');
          case "become_compliant_url":
          case "emissions_url":
          case "operation_hours_url":
          case "main_info_url":
          case "pricing_url":
          case "exemption_url":
          case "pay_caz_url":
          case "financial_assistance_url":
          case "boundary_url":
          case "additional_info_url":
            return SOME_URL;

        }
        throw new RuntimeException("Value not stubbed!");
      });

      when(resultSet.getBigDecimal(any())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case "bus_entrant_fee":
            return new BigDecimal("50.55");
          case "car_entrant_fee":
            return new BigDecimal("23.55");
          case "minibus_entrant_fee":
            return new BigDecimal("44.55");
          case "coach_entrant_fee":
            return new BigDecimal("50.00");
          case "taxi_entrant_fee":
            return new BigDecimal("15.10");
          case "phv_entrant_fee":
            return new BigDecimal("15.35");
          case "hgv_entrant_fee":
            return new BigDecimal("5.30");
          case "large_van_entrant_fee":
            return new BigDecimal("80.30");
          case "small_van_entrant_fee":
            return new BigDecimal("100.00");
          case "motorcycle_ent_fee":
            return new BigDecimal("25.10");
          case "moped_entrant_fee":
            return new BigDecimal("49.49");

        }
        throw new RuntimeException("Value not stubbed!");
      });

      return resultSet;
    }
  }

  private Tariff mockTariffInDB() {
    Tariff tariff = Tariff.builder()
        .cleanAirZoneId(SOME_CLEAN_AIR_ZONE_ID)
        .name("Leeds")
        .build();
    when(jdbcTemplate.queryForObject(anyString(), any(TariffRowMapper.class), any())).thenReturn(
        tariff);
    return tariff;
  }

  private Tariff expectedTariff() {
    InformationUrls informationUrls = InformationUrls.builder()
        .mainInfo(SOME_URL)
        .emissionsStandards(SOME_URL)
        .hoursOfOperation(SOME_URL)
        .pricing(SOME_URL)
        .exemptionOrDiscount(SOME_URL)
        .payCaz(SOME_URL)
        .becomeCompliant(SOME_URL)
        .financialAssistance(SOME_URL)
        .boundary(SOME_URL)
        .additionalInfo(SOME_URL)
        .build();
    Rates rates = Rates.builder()
        .bus(rate(50.55))
        .car(rate(23.55))
        .miniBus(rate(44.55))
        .coach(rate(50.00))
        .taxi(rate(15.10))
        .phv(rate(15.35))
        .hgv(rate(5.30))
        .largeVan(rate(80.30))
        .smallVan(rate(100.00))
        .motorcycle(rate(25.10))
        .moped(rate(49.49))
        .build();
    return Tariff.builder()
        .cleanAirZoneId(SOME_CLEAN_AIR_ZONE_ID)
        .name(LEEDS)
        .tariffClass('C')
        .chargeIdentifier(SOME_CHARGE_IDENTIFIER)
        .rates(rates)
        .informationUrls(informationUrls)
        .build();
  }

  private BigDecimal rate(double rate) {
    return new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP);
  }
}