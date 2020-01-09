package uk.gov.caz.taxiregister.repository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.testutils.TestObjects.LicensingAuthorities;

@ExtendWith(MockitoExtension.class)
class RegisterJobInfoRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private RegisterJobInfoRepository registerJobInfoRepository;

  @Captor
  private ArgumentCaptor<PreparedStatementCreator> preparedStmtCreatorArgumentCaptor;

  @Mock
  private Connection connection;

  @Mock
  private Array array;

  @Mock
  private PreparedStatement preparedStatement;

  @Test
  public void shouldSetParametersInPreparedStatement() throws SQLException {
    // given
    Set<LicensingAuthority> licensingAuthorities = LicensingAuthorities.existingAsSingleton();
    int registerJobId = 61;
    Integer[] licensingAuthoritiesIds = getLicensingAuthoritiesIds(licensingAuthorities);
    when(jdbcTemplate.update(preparedStmtCreatorArgumentCaptor.capture())).thenReturn(1);
    when(connection.prepareStatement(RegisterJobInfoRepository.INSERT_SQL))
        .thenReturn(preparedStatement);
    when(connection.createArrayOf("integer", licensingAuthoritiesIds)).thenReturn(array);

    // when
    registerJobInfoRepository.insert(registerJobId, licensingAuthorities);
    PreparedStatementCreator value = preparedStmtCreatorArgumentCaptor.getValue();
    value.createPreparedStatement(connection);

    // then
    verify(connection).prepareStatement(RegisterJobInfoRepository.INSERT_SQL);
    verify(connection).createArrayOf("integer", licensingAuthoritiesIds);
    verify(preparedStatement).setInt(1, registerJobId);
    verify(preparedStatement).setArray(2, array);
  }

  private Integer[] getLicensingAuthoritiesIds(Set<LicensingAuthority> licensingAuthorities) {
    return new Integer[]{licensingAuthorities.iterator().next().getId()};
  }
}