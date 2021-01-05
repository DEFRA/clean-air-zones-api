package uk.gov.caz.accounts.model.registerjob.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import uk.gov.caz.accounts.model.registerjob.RegisterJobError;
import uk.gov.caz.accounts.model.registerjob.RegisterJobErrors;

/**
 * Utility class which converts back and forth {@link RegisterJobErrors} to its database
 * representation.
 */
@Converter
@AllArgsConstructor
public class RegisterJobErrorsConverter implements AttributeConverter<RegisterJobErrors, String> {

  private final ObjectMapper objectMapper;

  @Override
  @SneakyThrows
  public String convertToDatabaseColumn(RegisterJobErrors registerJobErrors) {
    return objectMapper.writeValueAsString(registerJobErrors.getErrors());
  }

  @Override
  @SneakyThrows
  public RegisterJobErrors convertToEntityAttribute(String input) {
    return new RegisterJobErrors(
        objectMapper.readValue(input, new TypeReference<List<RegisterJobError>>() {})
    );
  }
}
