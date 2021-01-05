package uk.gov.caz.accounts.model.registerjob.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.registerjob.RegisterJobError;
import uk.gov.caz.accounts.model.registerjob.RegisterJobErrors;

@ExtendWith(MockitoExtension.class)
class RegisterJobErrorsConverterTest {

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private RegisterJobErrorsConverter registerJobErrorsConverter;

  @Nested
  class UponJsonConversionWhenConvertingToDatabaseColumnError {
    @Test
    public void shouldThrowJsonGenerationException() throws Exception {
      // given
      mockJsonExceptionWhenConvertingToJson();
      RegisterJobErrors registerJobErrors = new RegisterJobErrors(Collections.emptyList());

      // when
      Throwable throwable = catchThrowable(() ->
          registerJobErrorsConverter.convertToDatabaseColumn(registerJobErrors));

      // then
      assertThat(throwable).isInstanceOf(JsonGenerationException.class);
    }
  }

  @Nested
  class UponJsonConversionWhenConvertingFromDatabaseColumnError {
    @Test
    public void shouldThrowJsonGenerationException() throws Exception {
      // given
      mockJsonExceptionWhenConvertingFromJson();
      String input = "[]";

      // when
      Throwable throwable = catchThrowable(() ->
          registerJobErrorsConverter.convertToEntityAttribute(input));

      // then
      assertThat(throwable).isInstanceOf(JsonGenerationException.class);
    }
  }
  
  @Test
  public void registerJobErrorCountIdentified() {
    RegisterJobErrors registerJobWithoutErrors = new RegisterJobErrors(Collections.emptyList());
    assertThat(registerJobWithoutErrors.hasErrors()).isFalse();
    
    List<RegisterJobError> testErrors = new ArrayList<RegisterJobError>();
    testErrors.add(new RegisterJobError("Test vrn", "Test title", "Test detail"));
    RegisterJobErrors registerJobWithErrors = new RegisterJobErrors(testErrors);
    assertThat(registerJobWithErrors.hasErrors()).isTrue();
  }

  private void mockJsonExceptionWhenConvertingToJson() throws JsonProcessingException {
    given(objectMapper.writeValueAsString(anyList()))
        .willThrow(new JsonGenerationException("", (JsonGenerator) null));
  }

  private void mockJsonExceptionWhenConvertingFromJson() throws JsonProcessingException {
    given(objectMapper.readValue(any(String.class), any(TypeReference.class)))
        .willThrow(new JsonGenerationException("", (JsonGenerator) null));
  }
}