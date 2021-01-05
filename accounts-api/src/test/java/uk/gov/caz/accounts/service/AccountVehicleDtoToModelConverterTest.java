package uk.gov.caz.accounts.service;

import static org.assertj.core.api.BDDAssertions.then;
import static uk.gov.caz.accounts.util.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.ConversionResult;
import uk.gov.caz.accounts.model.ConversionResults;

class AccountVehicleDtoToModelConverterTest {

  private final AccountVehicleDtoToModelConverter converter = new AccountVehicleDtoToModelConverter();

  @Nested
  class WhenConvertingOneVehicle {

    @Test
    public void shouldReturnSuccessWhenConvertValidVehicle() {
      // given
      AccountVehicleDto accountVehicleDto = createValidAccountVehicleDto();

      // when
      ConversionResult conversionResult = converter
          .toAccountVehicle(accountVehicleDto, TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getAccountVehicle()).isNotNull();
    }

    @Test
    public void shouldReturnFailureWhenValidationFails() {
      // given
      AccountVehicleDto accountVehicleDto = createInvalidAccountVehicleDto();

      // when
      ConversionResult conversionResult = converter
          .toAccountVehicle(accountVehicleDto, TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getAccountVehicle()).isNull();
    }
  }

  @Nested
  class WhenConvertingListOfVehicles {

    @Test
    public void shouldReturnConvertedValidVehicles() {
      // given
      List<AccountVehicleDto> accountVehicleDtos = Collections.singletonList(
          createValidAccountVehicleDto());

      // when
      ConversionResults conversionResults = converter.convert(accountVehicleDtos,
          TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.size()).isEqualTo(1);
      conversionResults.getAccountVehicles()
          .stream()
          .forEach(vehicle -> {
            then(vehicle.getAccountId()).isEqualTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
          });
    }

    @Test
    public void shouldNotConvertUpToAnyThreshold() {
      // given
      List<AccountVehicleDto> vehicles = Arrays.asList(
          createValidAccountVehicleDto(),
          createInvalidAccountVehicleWithTwoValidationErrors(),
          createInvalidAccountVehicleWithTwoValidationErrors()
      );

      // when
      ConversionResults conversionResults = converter.convert(vehicles,
          TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      then(conversionResults.getValidationErrors()).hasSize(4);
      then(conversionResults.size()).isEqualTo(1);
    }

    @Test
    public void shouldReturnValidationErrorsAndConvertedVehicles() {
      // given
      List<AccountVehicleDto> accountVehicleDtos = Arrays.asList(
          createValidAccountVehicleDto(), createInvalidAccountVehicleDto()
      );

      // when
      ConversionResults conversionResults = converter.convert(accountVehicleDtos,
              TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
      then(conversionResults.size()).isEqualTo(1);
    }
  }

  private AccountVehicleDto createValidAccountVehicleDto() {
    return AccountVehicleDto.builder()
        .vrn("AAA999A")
        .build();
  }

  private AccountVehicleDto createInvalidAccountVehicleDto() {
    return AccountVehicleDto.builder()
        .vrn("AA_B-3$")
        .build();
  }

  private AccountVehicleDto createInvalidAccountVehicleWithTwoValidationErrors() {
    return AccountVehicleDto.builder()
        .vrn("$")
        .build();
  }
}