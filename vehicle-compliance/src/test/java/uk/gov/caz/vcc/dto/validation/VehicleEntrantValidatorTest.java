package uk.gov.caz.vcc.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;

class VehicleEntrantValidatorTest {

  @Test
  void shouldValidateObjectWithCustomValidator() {
    //given
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto(null, null);
    TestValidator testValidator = new TestValidator();

    //when
    List<ValidationError> validationErrors = testValidator.validate(vehicleEntrantDto);

    //then
    assertThat(validationErrors).hasSize(1);
  }

  private static class TestValidator implements VehicleEntrantValidator<String> {

    @Override
    public List<SingleFieldValidator<String>> getValidators() {
      return Lists.newArrayList(
          (index, field) -> field == null ? Optional.of(ValidationError.invalidVrnFormat("vrn"))
              : Optional.empty()
      );
    }

    @Override
    public String getValidatedField(VehicleEntrantDto vehicleEntrantDto) {
      return vehicleEntrantDto.getVrn();
    }
  }
}