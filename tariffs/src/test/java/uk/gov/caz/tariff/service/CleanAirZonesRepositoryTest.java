package uk.gov.caz.tariff.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.tariff.dto.CleanAirZone;
import uk.gov.caz.tariff.dto.CleanAirZones;
import uk.gov.caz.tariff.service.CleanAirZonesRepository.CleanAirZoneRowMapper;

@ExtendWith(MockitoExtension.class)
class CleanAirZonesRepositoryTest {

  private static final UUID SOME_CLEAN_AIR_ZONE_ID = UUID
      .fromString("dc1efcaf-a2cf-41ec-aa37-ea4b28a20a1d");

  private static final String SOME_URL = "www.test.uk";

  private  static final String LEEDS = "Leeds";

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private CleanAirZonesRepository cleanAirZonesRepository;

  @Test
  public void shouldFindCleanAirZones() {
    // given
    List<CleanAirZone> cleanAirZones = mockCleanAirZonesInDB();

    // when
    CleanAirZones result = cleanAirZonesRepository.findAll();

    // then
    assertThat(result.getCleanAirZones())
        .hasSize(2)
        .contains(cleanAirZones.get(0), cleanAirZones.get(1));
  }

  @Test
  public void shouldReturnEmptyList() {
    // given

    // when
    CleanAirZones result = cleanAirZonesRepository.findAll();

    // then
    assertThat(result.getCleanAirZones()).isEmpty();
  }

  @Nested
  class RowMapper {

    private CleanAirZoneRowMapper rowMapper = new CleanAirZoneRowMapper();

    @Test
    public void shouldMapResultSetToTariff() throws SQLException {
      ResultSet resultSet = mockResultSet();

      CleanAirZone cleanAirZone = rowMapper.mapRow(resultSet, 0);

      assertThat(cleanAirZone)
          .isNotNull()
          .isEqualToComparingFieldByFieldRecursively(expectedCleanAirZone());
    }

    private ResultSet mockResultSet() throws SQLException {
      ResultSet resultSet = mock(ResultSet.class);
      when(resultSet.getObject("clean_air_zone_id", UUID.class))
          .thenReturn(SOME_CLEAN_AIR_ZONE_ID);

      when(resultSet.getString(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case "caz_name":
            return LEEDS;
          case "boundary_url":
            return SOME_URL;
        }
        throw new RuntimeException("Value not stubbed!");
      });

      return resultSet;
    }
  }

  private List<CleanAirZone> mockCleanAirZonesInDB() {
    List<CleanAirZone> cleanAirZones = prepareCleanAirZones();
    when(jdbcTemplate.query(anyString(), any(CleanAirZoneRowMapper.class)))
        .thenReturn(cleanAirZones);
    return cleanAirZones;
  }

  private List<CleanAirZone> prepareCleanAirZones() {
    return new CleanAirZones(
        newArrayList(
            caz("Birmingham", "0d7ab5c4-5fff-4935-8c4e-56267c0c9493",
                "https://www.birmingham.gov.uk/info/20076/pollution/"
                    + "1763/a_clean_air_zone_for_birmingham/3"),

            caz("Leeds", "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3",
                "https://www.arcgis.com/home/webmap/viewer.html?webmap="
                    + "de0120ae980b473982a3149ab072fdfc&extent=-1.733%2c53.7378%2c-1.333%2c53.8621")
        )).getCleanAirZones();
  }

  private CleanAirZone caz(String cazName, String cleanAirZoneId, String boundaryUrl) {
    return CleanAirZone.builder()
        .name(cazName)
        .cleanAirZoneId(UUID.fromString(cleanAirZoneId))
        .boundaryUrl(URI.create(boundaryUrl))
        .build();
  }

  private CleanAirZone expectedCleanAirZone() {
    return CleanAirZone.builder()
        .cleanAirZoneId(SOME_CLEAN_AIR_ZONE_ID)
        .name(LEEDS)
        .boundaryUrl(URI.create(SOME_URL))
        .build();
  }
}