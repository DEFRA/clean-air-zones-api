package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;

@Component
public class VehicleToLicenceConverter {

  /**
   * Converts the passed list of {@link VehicleDto}s to {@link ConversionResults} with the respect
   * to the total number of validation errors that occur while converting. If {@code maxErrorsCount}
   * is zero, then no conversion takes place and an instance of {@link ConversionResults} is
   * returned with empty licences and validation errors.
   *
   * @param vehicles A list of {@link VehicleDto} which are to be mapped to a list of {@link
   *     TaxiPhvVehicleLicence} wrapped in {@link ConversionResults}.
   * @param maxErrorsCount The total number of validation errors that can happen during the
   *     conversion. If the size of validation errors reaches or exceeds this number the conversion
   *     is stopped immediately and the list of validation errors is truncated to satisfy the
   *     predicate: {@code ConversionResults.getValidationErrors().size() <= maxErrorsCount}.
   * @return An instance of {@link ConversionResults} which contains a list of converted vehicles
   *     into licences and a list of validation errors whose size does not exceed {@code
   *     maxErrorsCount} if {@code maxErrorsCount > 0}. If {@code maxErrorsCount == 0} an instance
   *     of {@link ConversionResults} with with empty licences and validation errors is returned.
   * @throws IllegalArgumentException if {@code maxErrorsCount < 0}.
   */
  public ConversionResults convert(List<VehicleDto> vehicles, int maxErrorsCount) {
    Preconditions.checkArgument(maxErrorsCount >= 0, "Expected maxErrorsCount >= 0, but %s < 0",
        maxErrorsCount);

    List<ConversionResult> conversionResults = Lists.newArrayListWithExpectedSize(vehicles.size());
    Iterator<VehicleDto> it = vehicles.iterator();
    int errorsCountLeft = maxErrorsCount;
    while (errorsCountLeft > 0 && it.hasNext()) {
      ConversionResult conversionResult = toLicence(it.next());
      if (conversionResult.isFailure()) {
        conversionResult = truncateValidationErrorsToMatchLimit(errorsCountLeft, conversionResult);
        errorsCountLeft -= conversionResult.getValidationErrors().size();
      }
      conversionResults.add(conversionResult);
    }
    return ConversionResults.from(conversionResults);
  }

  private ConversionResult truncateValidationErrorsToMatchLimit(int errorsCountLeft,
      ConversionResult conversionResult) {
    List<ValidationError> validationErrors = conversionResult.getValidationErrors();
    if (validationErrors.size() > errorsCountLeft) {
      // assertion: validationErrors.size() > errorsCountLeft > 0
      return ConversionResult.failure(validationErrors.subList(0, errorsCountLeft));
    }
    return conversionResult;
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
          .wheelchairAccessible(vehicleDto.getWheelchairAccessibleVehicle())
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
}