package uk.gov.caz.accounts.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.ConversionResult;
import uk.gov.caz.accounts.model.ConversionResults;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

@Component
public class AccountVehicleDtoToModelConverter {

  /**
   * Converts the passed list of {@link AccountVehicleDto}s to {@link ConversionResults}.
   *
   * @param vehicles A list of {@link AccountVehicleDto} which are to be mapped to a list of
   *     {@link AccountVehicle} wrapped in {@link ConversionResults}.
   * @param uploaderId uuid of person who update vehicles.
   * @return An instance of {@link ConversionResults} which contains a list of converted vehicles
   *     into vehicles and a list of validation errors.
   */
  public ConversionResults convert(List<AccountVehicleDto> vehicles, UUID uploaderId) {
    Set<String> uniqueVehicles = Sets.newHashSetWithExpectedSize(vehicles.size());
    List<ConversionResult> conversionResults = vehicles.stream()
        .map(vehicle -> convertAndValidate(uniqueVehicles, vehicle, uploaderId))
        .collect(Collectors.toList());
    return ConversionResults.from(conversionResults);
  }

  /**
   * Converts the passed instance of {@link AccountVehicleDto} to {@link ConversionResult}.
   */
  @VisibleForTesting
  ConversionResult toAccountVehicle(AccountVehicleDto vehicleDto, UUID uploaderId) {
    List<ValidationError> validationErrors = vehicleDto.validate();
    if (validationErrors.isEmpty()) {
      AccountVehicle accountVehicle = AccountVehicle.builder()
          .vrn(normaliseVrn(vehicleDto.getVrn()))
          .accountId(uploaderId)
          .build();
      return ConversionResult.success(accountVehicle);
    }
    return ConversionResult.failure(validationErrors);
  }

  private String normaliseVrn(String vrn) {
    return vrn.toUpperCase();
  }

  /**
   * Converts {@code vehicle} to an instance of {@link AccountVehicle} wrapped in {@link
   * ConversionResult} unless there are validation errors.
   */
  private ConversionResult convertAndValidate(Set<String> uniqueVehicles,
      AccountVehicleDto vehicle, UUID uploaderId) {
    ConversionResult conversionResult = toAccountVehicle(vehicle, uploaderId);
    if (conversionResult.isFailure()) {
      return conversionResult;
    }
    return getNonUniqueVehicleError(uniqueVehicles,
        conversionResult.getAccountVehicle().getVrn(), vehicle.getLineNumber())
        .map(conversionResult::withError)
        .orElse(conversionResult);
  }

  /**
   * Method that checks uniqueness of given vehicle in the set constructed out of CSV.
   */
  private Optional<ValidationError> getNonUniqueVehicleError(
      Set<String> uniqueVehicles, String vrn, int lineNumber) {
    boolean isNotUniqueVehicle = !uniqueVehicles.add(vrn);
    if (isNotUniqueVehicle) {
      return Optional.of(ValidationError.valueError(vrn,
          "There are multiple vehicles with the same VRN",
          lineNumber)
      );
    }
    return Optional.empty();
  }

}