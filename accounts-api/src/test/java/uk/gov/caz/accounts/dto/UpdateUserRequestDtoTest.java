package uk.gov.caz.accounts.dto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

public class UpdateUserRequestDtoTest {

  private UpdateUserRequestDto createDto(String name, List<String> permissions) {
    return UpdateUserRequestDto.builder()
        .name(name)
        .permissions(permissions)
        .build();
  }

  @Nested
  class ShouldNotProduceValidationErrorsForDtoWith {

    @Test
    public void nameOnly() {
      // given
      UpdateUserRequestDto dto = createDto("someName", null);

      // when
      noValidationErrorsWereProduced(dto);
    }

    @Test
    public void permissionsOnly() {
      // given
      UpdateUserRequestDto dto = createDto(null, Lists.newArrayList("permission1"));

      // when
      noValidationErrorsWereProduced(dto);
    }

    @Test
    public void permissionsAreEmptyList() {
      // given
      UpdateUserRequestDto dto = createDto(null, Lists.newArrayList());

      // when
      noValidationErrorsWereProduced(dto);
    }

    private void noValidationErrorsWereProduced(UpdateUserRequestDto dto) {
      assertDoesNotThrow( () -> {
        dto.validate();
      });
    }
  }

  @Nested
  class ShouldProduceValidationErrorsForDtoWith {


    @Test
    public void bothNameAndPermissionsMissing() {
      // given
      UpdateUserRequestDto dto = createDto(null, null);

      // when
      validationErrorsWereTriggered(dto);
    }

    private void validationErrorsWereTriggered(UpdateUserRequestDto dto) {
      assertThrows(InvalidRequestPayloadException.class, () -> {
        dto.validate();
      });
    }
  }

}