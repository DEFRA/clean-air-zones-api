package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;

/**
 * Converts the passed list of {@link VehicleDto}s to {@link ConversionResults} with the respect to
 * the total number of validation errors that occur while converting.
 */
@Component
public class VehicleToLicenceConverter {

  /**
   * Converts the passed list of {@link VehicleDto}s to {@link ConversionResults}.
   *
   * @param vehicles A list of {@link VehicleDto} which are to be mapped to a list of {@link
   *     TaxiPhvVehicleLicence} wrapped in {@link ConversionResults}.
   * @return An instance of {@link ConversionResults} which contains a list of converted vehicles
   *     into licences and a list of validation errors is returned.
   */
  public ConversionResults convert(List<VehicleDto> vehicles) {
    Set<UniqueLicenceAttributes> uniqueVehicles = Sets.newHashSetWithExpectedSize(vehicles.size());
    List<ConversionResult> conversionResults = Lists.newArrayListWithExpectedSize(vehicles.size());

    for (VehicleDto vehicle : vehicles) {
      ConversionResult conversionResult = convertAndValidate(uniqueVehicles, vehicle);
      conversionResults.add(conversionResult);
    }
    return ConversionResults.from(conversionResults);
  }

  private ConversionResult convertAndValidate(Set<UniqueLicenceAttributes> uniqueVehicles,
      VehicleDto vehicle) {
    ConversionResult conversionResult = toLicence(vehicle);
    if (conversionResult.isFailure()) {
      return conversionResult;
    }
    return getNonUniqueVehicleError(uniqueVehicles, conversionResult.getLicence())
        .map(conversionResult::withError)
        .orElse(conversionResult);
  }

  /**
   * Converts the passed instance of {@link VehicleDto} to {@link ConversionResult}.
   */
  @VisibleForTesting
  ConversionResult toLicence(VehicleDto vehicleDto) {
    List<ValidationError> validationResult = vehicleDto.validate();
    if (validationResult.isEmpty()) {

      TaxiPhvVehicleLicence licence = TaxiPhvVehicleLicence.builder()
          .vrm(vehicleDto.getVrm())
          .licenseDates(
              new LicenseDates(
                  parseDateFromSupportedValue(vehicleDto.getStart()),
                  parseDateFromSupportedValue(vehicleDto.getEnd())
              )
          )
          .description(vehicleDto.getDescription())
          .licensingAuthority(
              LicensingAuthority.withNameOnly(vehicleDto.getLicensingAuthorityName()))
          .licensePlateNumber(vehicleDto.getLicensePlateNumber())
          .wheelchairAccessible(toBoolean(vehicleDto.getWheelchairAccessibleVehicle()))
          .build();
      return ConversionResult.success(licence);
    }
    return ConversionResult.failure(validationResult);
  }

  /**
   * Parses a local date object from a supported string format.
   */
  private LocalDate parseDateFromSupportedValue(String rawDateInput) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter
          .ofPattern("yyyy-MM-dd");
      return LocalDate.parse(rawDateInput, formatter);
    } catch (DateTimeParseException e) {
      DateTimeFormatter formatter = DateTimeFormatter
          .ofPattern("dd/MM/yyyy");
      return LocalDate.parse(rawDateInput, formatter);
    }
  }

  /**
   * Maps the optional boolean value parameter (represented by a string, which is already validated,
   * i.e. it is either blank or null or equal to true or false in any capitalization) to {@link
   * Boolean}.
   *
   * @param booleanValue An optional (nullable) boolean value represented as a string in any
   *     capitalization.
   * @return {@code null} if {@code booleanValue} is null or empty, a converted value to {@link
   *     Boolean#TRUE}/{@link Boolean#FALSE} otherwise.
   */
  private Boolean toBoolean(String booleanValue) {
    return Optional.ofNullable(Strings.emptyToNull(booleanValue))
        .map(String::toLowerCase)
        .map(Boolean::valueOf)
        .orElse(null);
  }

  /**
   * Method that checks uniqueness of given vehicle in the set constructed out of CSV.
   */
  private Optional<ValidationError> getNonUniqueVehicleError(
      Set<UniqueLicenceAttributes> uniqueVehicles, TaxiPhvVehicleLicence licence) {
    Preconditions.checkNotNull(licence);
    boolean isNotUniqueVehicle = !uniqueVehicles.add(UniqueLicenceAttributes.from(licence));
    if (isNotUniqueVehicle) {
      return Optional.of(ValidationError.valueError(licence.getVrm(),
          "There are multiple vehicles with the same VRN")
      );
    }
    return Optional.empty();
  }
}