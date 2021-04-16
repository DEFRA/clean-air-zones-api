package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_EMAIL;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.whitelist.model.Actions.CREATE;
import static uk.gov.caz.whitelist.model.Actions.DELETE;
import static uk.gov.caz.whitelist.model.Actions.UPDATE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.dto.validation.DatabaseCheckValidator;
import uk.gov.caz.whitelist.model.ConversionResult;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;

@ExtendWith(MockitoExtension.class)
class WhitelistedVehicleDtoToModelConverterTest {

  @Mock
  private WhitelistVehiclePostgresRepository postgresRepository;

  @Mock
  private DatabaseCheckValidator databaseCheckValidator;

  @InjectMocks
  private WhitelistedVehicleDtoToModelConverter converter;

  @Nested
  class WhenConvertingOneVehicle {

    @Test
    public void shouldReturnSuccessWhenConvertValidVehicle() {
      // given
      WhitelistedVehicleDto retrofittedVehicle = createValidWhitelistedVehicle();

      // when
      ConversionResult conversionResult = converter
          .toWhitelistedVehicle(retrofittedVehicle, TYPICAL_REGISTER_JOB_UPLOADER_ID,
              TYPICAL_EMAIL);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getWhitelistVehicleCommand()).isNotNull();
      then(conversionResult.getWhitelistVehicleCommand().getWhitelistVehicle().getUploaderId())
          .isEqualTo(
              TYPICAL_REGISTER_JOB_UPLOADER_ID);
    }

    @Test
    public void shouldReturnFailureWhenValidationFails() {
      // given
      WhitelistedVehicleDto retrofittedVehicle = createInvalidWhitelistedVehicle();

      // when
      ConversionResult conversionResult = converter
          .toWhitelistedVehicle(retrofittedVehicle, TYPICAL_REGISTER_JOB_UPLOADER_ID,
              TYPICAL_EMAIL);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getWhitelistVehicleCommand()).isNull();
    }

    @Test
    public void shouldReturnFailureWhenCreateOnExistingVrn() {
      // given
      WhitelistedVehicleDto retrofittedVehicle = createValidWhitelistedVehicleToBeCreated();
      mockDatabaseCheckValidator(true, retrofittedVehicle);

      // when
      ConversionResult conversionResult = converter
          .toWhitelistedVehicle(retrofittedVehicle, TYPICAL_REGISTER_JOB_UPLOADER_ID,
              TYPICAL_EMAIL);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getWhitelistVehicleCommand()).isNull();
    }

    @Test
    public void shouldReturnFailureWhenDeleteOnNonExistingVrn() {
      // given
      WhitelistedVehicleDto retrofittedVehicle = createValidWhitelistedVehicleToBeDeleted();
      mockDatabaseCheckValidator(false, retrofittedVehicle);

      // when
      ConversionResult conversionResult = converter
          .toWhitelistedVehicle(retrofittedVehicle, TYPICAL_REGISTER_JOB_UPLOADER_ID,
              TYPICAL_EMAIL);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getWhitelistVehicleCommand()).isNull();
    }
  }

  @Nested
  class WhenConvertingListOfVehicles {

    @Test
    public void shouldReturnConvertedValidVehicles() {
      // given)
      List<WhitelistedVehicleDto> whitelistedVehicleDtos = Collections.singletonList(
          createValidWhitelistedVehicle());

      // when
      ConversionResults conversionResults = converter
          .convert(whitelistedVehicleDtos, TYPICAL_REGISTER_JOB_UPLOADER_ID, TYPICAL_EMAIL);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.size()).isEqualTo(1);
      conversionResults.getWhitelistVehiclesToSaveOrUpdate()
          .stream()
          .forEach(vehicle -> {
            then(vehicle.getUploaderId()).isEqualTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
          });
    }

    @Test
    public void shouldReturnValidationErrorsAndConvertedVehicles() {
      // given
      List<WhitelistedVehicleDto> whitelistedVehicleDtos = Arrays.asList(
          createValidWhitelistedVehicle(), createInvalidWhitelistedVehicle()
      );

      // when
      ConversionResults conversionResults = converter
          .convert(whitelistedVehicleDtos, TYPICAL_REGISTER_JOB_UPLOADER_ID, TYPICAL_EMAIL);

      // then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
      then(conversionResults.size()).isEqualTo(1);
    }
  }

  private WhitelistedVehicleDto createValidWhitelistedVehicle() {
    return WhitelistedVehicleDto.builder()
        .vrn("AAA999A")
        .manufacturer(Optional.of("manufacturer"))
        .category("Early Adopter")
        .reason("Reason")
        .action(UPDATE.getActionCharacter())
        .build();
  }

  private WhitelistedVehicleDto createInvalidWhitelistedVehicle() {
    return WhitelistedVehicleDto.builder()
        .manufacturer(Optional.of("manufacturer"))
        .category("Early Adopter")
        .reason("Reason")
        .action(UPDATE.getActionCharacter())
        .build();
  }

  private WhitelistedVehicleDto createValidWhitelistedVehicleToBeCreated() {
    return WhitelistedVehicleDto.builder()
        .vrn("TEST")
        .manufacturer(Optional.of("manufacturer"))
        .category("Early Adopter")
        .reason("Reason")
        .action(CREATE.getActionCharacter())
        .build();
  }

  private WhitelistedVehicleDto createValidWhitelistedVehicleToBeDeleted() {
    return WhitelistedVehicleDto.builder()
        .vrn("TEST")
        .manufacturer(Optional.of("manufacturer"))
        .category("Early Adopter")
        .reason("Reason")
        .action(DELETE.getActionCharacter())
        .build();
  }

  private WhitelistedVehicleDto createInvalidWhitelistedVehicleWithThreeValidationErrors() {
    return WhitelistedVehicleDto.builder()
        .lineNumber(1)
        .build();
  }

  private WhitelistedVehicleDto createInvalidWhitelistedVehicleWithTwoValidationErrors() {
    return WhitelistedVehicleDto.builder()
        .action(UPDATE.getActionCharacter())
        .lineNumber(1)
        .build();
  }

  @SneakyThrows
  private void mockDatabaseCheckValidator(boolean isOnWhitelist, WhitelistedVehicleDto dto) {
    String action = dto.getAction();
    String vrn = dto.getVrn();
    if (isOnWhitelist && action.equals(CREATE.getActionCharacter())) {
      Mockito.when(databaseCheckValidator.validate(dto))
          .thenReturn(Arrays.asList(ValidationError.valueError(vrn,
              "You can't add that number plate as it already exists in the database.")));
    } else if (!isOnWhitelist && action.equals(DELETE.getActionCharacter())) {
      Mockito.when(databaseCheckValidator.validate(dto))
          .thenReturn(Arrays.asList(ValidationError.valueError(vrn,
              "You can't delete that number plate as it doesn’t exist in the database.")));
    } else {
      Mockito.when(databaseCheckValidator.validate(dto))
          .thenReturn(Collections.emptyList());
    }
  }
}