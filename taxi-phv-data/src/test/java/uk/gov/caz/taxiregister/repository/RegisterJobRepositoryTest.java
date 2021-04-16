package uk.gov.caz.taxiregister.repository;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.caz.taxiregister.repository.RegisterJobRepository.COL_CORRELATION_ID;
import static uk.gov.caz.taxiregister.repository.RegisterJobRepository.COL_ERRORS;
import static uk.gov.caz.taxiregister.repository.RegisterJobRepository.COL_JOB_NAME;
import static uk.gov.caz.taxiregister.repository.RegisterJobRepository.COL_REGISTER_JOB_ID;
import static uk.gov.caz.taxiregister.repository.RegisterJobRepository.COL_STATUS;
import static uk.gov.caz.taxiregister.repository.RegisterJobRepository.COL_TRIGGER;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_TRIGGER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_ERRORS_JOINED;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_RUNNING_REGISTER_JOB_STATUS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.repository.RegisterJobRepository.RegisterJobRowMapper;
import uk.gov.caz.taxiregister.util.JsonHelpers;

class RegisterJobRepositoryTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final JsonHelpers jsonHelpers = new JsonHelpers(objectMapper);

  private RegisterJobRepository registerJobRepository;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  public void initialize() {
    jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    registerJobRepository = new RegisterJobRepository(jdbcTemplate, jsonHelpers, objectMapper);
  }

  @Nested
  class UpdateErrors {

    @Test
    public void shouldThrowNullPointerExceptionWhenPassedListIsNull() {
      // given
      int registerJobId = 12;
      List<RegisterJobError> errors = null;

      // when
      Throwable throwable = catchThrowable(() ->
          registerJobRepository.updateErrors(registerJobId, errors));

      // then
      then(throwable).isInstanceOf(NullPointerException.class);
      verifyZeroInteractions(jdbcTemplate);
    }

    @Test
    public void shouldUpdateErrorsToNullWhenPassedListIsEmpty() {
      // given
      int registerJobId = 13;
      List<RegisterJobError> errors = Collections.emptyList();

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      verify(jdbcTemplate).update(anyString(), eq(null), eq(registerJobId));
    }

    @Test
    public void shouldSaveJsonListWithAllParameters() {
      // given
      int registerJobId = 12;
      String vrm = "123";
      String title = "Validation error";
      String detail = "some error";
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError(vrm,
          title, detail));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("vrm", vrm))
          .append(',')
          .append(jsonField("title", title))
          .append(',')
          .append(jsonField("detail", detail))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    @Test
    public void shouldSaveJsonListWithoutVrmWhenVrmIsNull() {
      // given
      int registerJobId = 12;
      String vrm = null;
      String title = "Validation error";
      String detail = "some error";
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError(vrm,
          title, detail));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("title", title))
          .append(',')
          .append(jsonField("detail", detail))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    @Test
    public void shouldSaveJsonListWithoutVrmWhenVrmIsEmpty() {
      // given
      int registerJobId = 12;
      String vrm = "";
      String title = "Validation error";
      String detail = "some error";
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError(vrm,
          title, detail));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("title", title))
          .append(',')
          .append(jsonField("detail", detail))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    private String jsonField(String key, String value) {
      return String.format("\"%s\":\"%s\"", key, value);
    }

  }

  @Nested
  class RowMapper {

    private RegisterJobRowMapper rowMapper = new RegisterJobRowMapper(new ObjectMapper());

    @Test
    public void shouldMapResultSetToRegisterJobWithAnyValidValues() throws SQLException {
      // given
      ResultSet resultSet = mockResultSetWithAnyValidValues();

      // when
      RegisterJob registerJob = rowMapper.mapRow(resultSet, 0);

      // then
      assertThat(registerJob)
          .isNotNull()
          .matchesAttributesOfTypicalRunningRegisterJob();
    }

    @Test
    public void shouldMapResultSetToRegisterJobWithErrorsSetToNullWhenThereAreNoErrors()
        throws SQLException {
      // given
      ResultSet resultSet = mockResultSetWithErrorsEqualTo(null);

      // when
      RegisterJob registerJob = rowMapper.mapRow(resultSet, 0);

      // then
      assertThat(registerJob)
          .isNotNull()
          .hasErrors(Collections.emptyList());
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenParsingErrorFails()
        throws SQLException, IOException {
      // given
      mockInputOutputExceptionWhenParsingJson();
      ResultSet resultSet = mockResultSetWithErrorsEqualTo("");

      // when
      Throwable throwable = catchThrowable(() -> rowMapper.mapRow(resultSet, 0));

      // then
      assertThat(throwable)
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(IOException.class);
    }

    private ResultSet mockResultSetWithAnyValidValues() throws SQLException {
      return mockResultSetWithErrorsEqualTo(TYPICAL_REGISTER_JOB_ERRORS_JOINED);
    }

    private ResultSet mockResultSetWithErrorsEqualTo(String errors) throws SQLException {
      ResultSet resultSet = mock(ResultSet.class);

      when(resultSet.getInt(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case COL_REGISTER_JOB_ID:
            return S3_REGISTER_JOB_ID;
        }
        throw new RuntimeException("Value not stubbed!");
      });

      when(resultSet.getObject(anyString(), (Class<?>) any(Class.class)))
          .thenAnswer(answer -> TYPICAL_REGISTER_JOB_UPLOADER_ID);

      when(resultSet.getString(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case COL_TRIGGER:
            return S3_REGISTER_JOB_TRIGGER.name();
          case COL_JOB_NAME:
            return S3_REGISTER_JOB_NAME;
          case COL_STATUS:
            return TYPICAL_RUNNING_REGISTER_JOB_STATUS.name();
          case COL_ERRORS:
            return errors;
          case COL_CORRELATION_ID:
            return TYPICAL_CORRELATION_ID;
        }
        throw new RuntimeException("Value not stubbed!");
      });
      return resultSet;
    }
  }

  private void mockInputOutputExceptionWhenParsingJson() throws IOException {
    ObjectMapper om = mock(ObjectMapper.class);
    given(om.readValue(anyString(), any(TypeReference.class))).willThrow(new RuntimeException());
    registerJobRepository = new RegisterJobRepository(jdbcTemplate, jsonHelpers, objectMapper);
  }
}
