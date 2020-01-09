package uk.gov.caz.auditcleanup;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.caz.auditcleanup.AuditPostgresRepository.DELETE_SQL;

import java.sql.Types;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AuditPostgresRepositoryTest {

  @Mock
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @InjectMocks
  private AuditPostgresRepository auditPostgresRepository;

  @Test
  public void shouldCallUpdateMethodWithProperValues() {
    //given
    LocalDate date = LocalDate.now();

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("input_date", date, Types.DATE);

    when(namedParameterJdbcTemplate
        .update(ArgumentMatchers.eq(DELETE_SQL), ArgumentMatchers.any(MapSqlParameterSource.class)))
        .thenReturn(1);

    //when
    auditPostgresRepository.removeAuditEventsBeforeDate(date);

    //then
    verify(namedParameterJdbcTemplate)
        .update(ArgumentMatchers.eq(DELETE_SQL), ArgumentMatchers.refEq(mapSqlParameterSource));
  }
}