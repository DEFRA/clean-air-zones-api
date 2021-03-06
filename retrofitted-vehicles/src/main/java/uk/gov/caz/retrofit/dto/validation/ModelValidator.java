package uk.gov.caz.retrofit.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

public class ModelValidator implements RetrofittedVehicleValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 30;

  @VisibleForTesting
  static final String INVALID_MODEL_MESSAGE_TEMPLATE = "'model' should have "
      + "from 1 to %d characters instead of %d.";

  @Override
  public List<ValidationError> validate(RetrofittedVehicleDto retrofittedVehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    ModelErrorResolver errorMessageResolver = new ModelErrorResolver(retrofittedVehicleDto);

    String vrn = retrofittedVehicleDto.getVrn();
    String model = retrofittedVehicleDto.getModel();

    if (!Strings.isNullOrEmpty(model) && model.length() > MAX_LENGTH) {
      validationErrorsBuilder.add(errorMessageResolver.invalidFormat(vrn, model.length()));
    }

    return validationErrorsBuilder.build();
  }

  private static class ModelErrorResolver extends ValidationErrorResolver {

    private ModelErrorResolver(RetrofittedVehicleDto retrofittedVehicleDto) {
      super(retrofittedVehicleDto);
    }

    private ValidationError invalidFormat(String vrn, int modelLength) {
      return valueError(vrn, invalidFormatMessage(modelLength));
    }

    private String invalidFormatMessage(int modelLength) {
      return String.format(INVALID_MODEL_MESSAGE_TEMPLATE, MAX_LENGTH, modelLength);
    }
  }
}
