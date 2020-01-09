package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

public class LicenceDatesValidator implements LicenceValidator {

  @VisibleForTesting
  static final String MISSING_DATE_MESSAGE_TEMPLATE = "Missing %s date";

  @VisibleForTesting
  static final String INVALID_DATE_FORMAT_TEMPLATE 
      = "Invalid %s date format. Date format must be either ISO (YYYY-MM-DD) or DD/MM/YYYY";

  @VisibleForTesting
  static final String INVALID_ORDER_DATE_MESSAGE = "Start date must be before end date";

  @VisibleForTesting
  static final String START_DATE_TOO_FAR_IN_PAST
      = "Start date cannot be more than 20 years in the past";
  
  @VisibleForTesting
  static final String END_DATE_TOO_FAR_IN_FUTURE
      = "End date cannot be more than 20 years in the future";

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicenceDatesErrorMessageResolver errorResolver = new LicenceDatesErrorMessageResolver(
        vehicleDto);

    String vrm = vehicleDto.getVrm();
    String start = vehicleDto.getStart();
    String end = vehicleDto.getEnd();

    LocalDate convertedStartDate = tryParsingDate(start, "start", vrm, errorResolver,
        validationErrorsBuilder, vehicleDto.getRegisterJobTrigger());
    LocalDate convertedEndDate = tryParsingDate(end, "end", vrm, errorResolver,
        validationErrorsBuilder, vehicleDto.getRegisterJobTrigger());

    if (convertedStartDate != null && convertedEndDate != null) {
      if (convertedEndDate.isBefore(convertedStartDate)) {
        validationErrorsBuilder.add(errorResolver.invalidOrder(vrm));
      } else {
        if (!startDateWithinLast20Years(convertedStartDate)) {
          validationErrorsBuilder.add(errorResolver.startDateTooFarInPast(vrm));
        }

        if (!endDateWithinNext20Years(convertedEndDate)) {
          validationErrorsBuilder.add(errorResolver.endDateTooFarInFuture(vrm));
        }
      }
    }

    return validationErrorsBuilder.build();
  }

  private LocalDate tryParsingDate(String stringifiedDate,
      String startOrEnd,
      String vrm,
      LicenceDatesErrorMessageResolver errorResolver,
      Builder<ValidationError> validationErrorsBuilder,
      RegisterJobTrigger jobTrigger) {
    LocalDate convertedDate = null;
    if (Strings.isNullOrEmpty(stringifiedDate)) {
      validationErrorsBuilder.add(errorResolver.missing(startOrEnd, vrm));
    } else {
      // CSV submissions may accept dd/MM/yyyy or yyyy-MM-dd format.
      // This will run both formats for CSV_FROM_S3, but only the latter
      // for API_CALL submissions.
      if (jobTrigger.equals(RegisterJobTrigger.CSV_FROM_S3)) {
        convertedDate = parseDate(stringifiedDate, "dd/MM/yyyy");
      }

      if (convertedDate == null) {
        convertedDate = parseDate(stringifiedDate, "yyyy-MM-dd");
      }

      if (convertedDate == null) {
        validationErrorsBuilder.add(errorResolver.invalidFormat(startOrEnd, vrm));
      }
    }
    return convertedDate;
  }

  private LocalDate parseDate(String stringifiedDate, String format) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
      return LocalDate.parse(stringifiedDate, formatter);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private boolean startDateWithinLast20Years(LocalDate startDate) {
    LocalDate twentyYearsAgo = LocalDate.now().minusYears(20);
    return startDate.isAfter(twentyYearsAgo);
  }

  private boolean endDateWithinNext20Years(LocalDate endDate) {
    LocalDate twentyYearsAhead = LocalDate.now().plusYears(20);
    return endDate.isBefore(twentyYearsAhead);
  }

  private static class LicenceDatesErrorMessageResolver extends ValidationErrorResolver {

    private LicenceDatesErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String startOrEnd, String vrm) {
      return missingFieldError(vrm, missingDateMessage(startOrEnd));
    }

    private ValidationError invalidFormat(String startOrEnd, String vrm) {
      return valueError(vrm, invalidDateFormatMessage(startOrEnd));
    }

    private ValidationError startDateTooFarInPast(String vrm) {
      return valueError(vrm, START_DATE_TOO_FAR_IN_PAST);
    }

    private ValidationError endDateTooFarInFuture(String vrm) {
      return valueError(vrm, END_DATE_TOO_FAR_IN_FUTURE);
    }

    private ValidationError invalidOrder(String vrm) {
      return valueError(vrm, INVALID_ORDER_DATE_MESSAGE);
    }

    private String invalidDateFormatMessage(String startOrEnd) {
      return String.format(INVALID_DATE_FORMAT_TEMPLATE, startOrEnd);
    }

    private String missingDateMessage(String startOrEnd) {
      return String.format(MISSING_DATE_MESSAGE_TEMPLATE, startOrEnd);
    }
  }
}
