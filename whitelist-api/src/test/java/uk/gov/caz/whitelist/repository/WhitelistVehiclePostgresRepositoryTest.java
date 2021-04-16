package uk.gov.caz.whitelist.repository;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository.WhitelistVehicleRowMapper;

@ExtendWith(MockitoExtension.class)
class WhitelistVehiclePostgresRepositoryTest {

  private static final int ANY_BATCH_SIZE = 1;

  public static final WhitelistVehicle MILITARY_VEHICLE_1 = WhitelistVehicle.builder()
      .vrn("8839GF")
      .reasonUpdated("reasonUpdated1")
      .manufacturer("manufacturer1")
      .updateTimestamp(LocalDateTime.now())
      .uploaderId(UUID.randomUUID())
      .build();

  public static final WhitelistVehicle NORMAL_VEHICLE_1 = WhitelistVehicle.builder()
      .vrn("1839GF")
      .reasonUpdated("reasonUpdated1")
      .manufacturer("manufacturer1")
      .updateTimestamp(LocalDateTime.now())
      .uploaderId(UUID.randomUUID())
      .build();

  private static final Set<WhitelistVehicle> RETROFITTED_VEHICLES = Sets.newHashSet(
      MILITARY_VEHICLE_1,
      NORMAL_VEHICLE_1
  );

  private WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @BeforeEach
  void setup() {
    whitelistVehiclePostgresRepository = new WhitelistVehiclePostgresRepository(
        jdbcTemplate, namedParameterJdbcTemplate, ANY_BATCH_SIZE
    );
  }

  @Nested
  class SaveOrUpdate {

    @Test
    void shouldInsertInBatches() {
      //given
      Set<WhitelistVehicle> whitelistVehicles = RETROFITTED_VEHICLES;

      //when
      whitelistVehiclePostgresRepository.saveOrUpdate(whitelistVehicles);

      //then
      verify(jdbcTemplate, times(2))
          .batchUpdate(eq(WhitelistVehiclePostgresRepository.INSERT_OR_UPDATE_SQL),
              any(BatchPreparedStatementSetter.class));
    }
  }

  @Nested
  class FindOneByVrn {

    @Test
    void shouldThrowExceptionIfMultipleRecordFound() {
      //given
      when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class),
          any(WhitelistVehicleRowMapper.class))).thenReturn(
          Arrays.asList(WhitelistVehicle.builder().build(),
              WhitelistVehicle.builder().build()));

      // when
      Throwable throwable = catchThrowable(
          () -> whitelistVehiclePostgresRepository.findOneByVrn("any"));

      // then
      BDDAssertions.then(throwable).isInstanceOf(ApplicationRuntimeException.class);
    }

    @Test
    void shouldThrowExceptionIfVrnIsNull() {
      //given
      // when
      Throwable throwable = catchThrowable(
          () -> whitelistVehiclePostgresRepository.findOneByVrn(null));

      // then
      BDDAssertions.then(throwable).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class DeleteByVrnsIn {

    @Test
    void shouldThrowExceptionIfVrnListIsEmpty() {
      //given
      // when
      Throwable throwable = catchThrowable(
          () -> whitelistVehiclePostgresRepository.deleteByVrnsIn(emptySet()));

      // then
      BDDAssertions.then(throwable).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Exists {

    @Test
    public void shouldThrowExceptionWhenVrnIsNull() {
      // given
      String vrn = null;

      // when
      Throwable throwable = catchThrowable(() -> whitelistVehiclePostgresRepository.exists(vrn));

      // then
      BDDAssertions.then(throwable).isInstanceOf(IllegalArgumentException.class);
    }
  }
}