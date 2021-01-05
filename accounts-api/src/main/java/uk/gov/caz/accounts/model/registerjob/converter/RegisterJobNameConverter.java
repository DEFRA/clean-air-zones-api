package uk.gov.caz.accounts.model.registerjob.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;

/**
 * Utility class which converts back and forth {@link RegisterJobName} to its database
 * representation.
 */
@Converter
public class RegisterJobNameConverter implements AttributeConverter<RegisterJobName, String> {

  @Override
  public String convertToDatabaseColumn(RegisterJobName registerJobName) {
    return registerJobName.getValue();
  }

  @Override
  public RegisterJobName convertToEntityAttribute(String input) {
    return new RegisterJobName(input);
  }
}
