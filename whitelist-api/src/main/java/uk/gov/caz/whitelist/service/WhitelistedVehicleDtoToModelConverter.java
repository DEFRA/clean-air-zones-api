package uk.gov.caz.whitelist.service;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.dto.validation.DatabaseCheckValidator;
import uk.gov.caz.whitelist.model.ConversionResult;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.model.WhitelistVehicleCommand;

@Component
@AllArgsConstructor
public class WhitelistedVehicleDtoToModelConverter {

  private final DatabaseCheckValidator databaseCheckValidator;

  /**
   * Converts the passed list of {@link WhitelistedVehicleDto}s to {@link ConversionResults} with
   * the respect to the total number of validation errors that occur while converting.
   *
   * @param vehicles A list of {@link WhitelistedVehicleDto} which are to be mapped to a list of
   *     {@link WhitelistVehicle} wrapped in {@link ConversionResults}.
   * @param uploaderId uuid of person who update vehicles.
   * @param email value of uploader email.
   * @return An instance of {@link ConversionResults} which contains a list of converted vehicles
   *     into vehicles and a list of validation errors whose size does not exceed {@code
   *     maxErrorsCount} if {@code maxErrorsCount > 0}. If {@code maxErrorsCount == 0} an instance
   *     of {@link ConversionResults} with with empty vehicles and validation errors is returned.
   * @throws IllegalArgumentException if {@code maxErrorsCount < 0}.
   */
  public ConversionResults convert(List<WhitelistedVehicleDto> vehicles,
      UUID uploaderId, String email) {

    List<ConversionResult> convertedVehicles = vehicles.stream()
        .map(vehicle -> toWhitelistedVehicle(vehicle, uploaderId, email))
        .collect(Collectors.toList());
    return ConversionResults.from(convertedVehicles);
  }

  /**
   * Converts the passed instance of {@link WhitelistedVehicleDto} to {@link ConversionResult}.
   */
  @VisibleForTesting
  ConversionResult toWhitelistedVehicle(WhitelistedVehicleDto vehicleDto, UUID uploaderId,
      String email) {
    List<ValidationError> validationResult = vehicleDto
        .validateWithAdditionaValidator(databaseCheckValidator);

    if (validationResult.isEmpty()) {
      WhitelistVehicle whitelistVehicle = vehicleDto.mapToWhitelistVehicle(uploaderId, email);
      return ConversionResult.success(WhitelistVehicleCommand.builder()
          .action(vehicleDto.getAction()).whitelistVehicle(whitelistVehicle).build());
    }
    return ConversionResult.failure(validationResult);
  }
}