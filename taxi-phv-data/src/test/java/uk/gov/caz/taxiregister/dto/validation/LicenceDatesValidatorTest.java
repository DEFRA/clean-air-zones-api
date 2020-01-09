package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

class LicenceDatesValidatorTest {

  private static final String MISSING_START_DATE_MSG = String.format(
      LicenceDatesValidator.MISSING_DATE_MESSAGE_TEMPLATE, "start"
  );
  private static final String MISSING_END_DATE_MSG = String.format(
      LicenceDatesValidator.MISSING_DATE_MESSAGE_TEMPLATE, "end"
  );
  private static final String INVALID_START_DATE_FORMAT_MSG = String.format(
      LicenceDatesValidator.INVALID_DATE_FORMAT_TEMPLATE, "start"
  );
  private static final String INVALID_END_DATE_FORMAT_MSG = String.format(
      LicenceDatesValidator.INVALID_DATE_FORMAT_TEMPLATE, "end"
  );

  private static final String ANY_VRM = "ZC62OMB";

  private final LicenceDatesValidator validator = new LicenceDatesValidator();

  @Nested
  class MandatoryFields {

    @Nested
    class WithLineNumber {

      @Test
      public void shouldReturnMissingFieldErrorWhenStartDateIsNull() {
        // given
        int lineNumber = 71;
        String start = null;
        String end = "2019-05-17";
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, missingStartDateMsg(), lineNumber));
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenStartDateIsEmpty() {
        // given
        int lineNumber = 71;
        String start = "";
        String end = "2019-05-17";
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, missingStartDateMsg(), lineNumber));
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenEndDateIsNull() {
        // given
        int lineNumber = 72;
        String start = "2019-05-17";
        String end = null;
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, missingEndDateMsg(), lineNumber));
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenEndDateIsEmpty() {
        // given
        int lineNumber = 72;
        String start = "2019-05-17";
        String end = "";
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, missingEndDateMsg(), lineNumber));
      }


      @Test
      public void shouldReturnMissingFieldErrorWhenBothDatesAreNull() {
        // given
        int lineNumber = 73;
        String start = null;
        String end = null;
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, missingStartDateMsg(), lineNumber),
            ValidationError.missingFieldError(ANY_VRM, missingEndDateMsg(), lineNumber)
        );
      }
    }
    @Nested
    class WithoutLineNumber {

      @Test
      public void shouldReturnMissingFieldErrorWhenStartDateIsNull() {
        // given
        String start = null;
        String end = "2019-05-17";
        VehicleDto licence = createLicence(start, end);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, MISSING_START_DATE_MSG));
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenEndDateIsNull() {
        // given
        String start = "2019-05-17";
        String end = null;
        VehicleDto licence = createLicence(start, end);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, MISSING_END_DATE_MSG));
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenBothDatesAreNull() {
        // given
        String start = null;
        String end = null;
        VehicleDto licence = createLicence(start, end);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, MISSING_START_DATE_MSG),
            ValidationError.missingFieldError(ANY_VRM, MISSING_END_DATE_MSG)
        );
      }

    }
  }
  @Nested
  class Format {

    @Nested
    class WithoutLineNumber {

      @Test
      public void shouldReturnValueErrorsWhenBothDatesHaveInvalidFormat() {
        // given
        String start = "2019-05-17-01";
        String end = "01/13/2019";
        VehicleDto licence = createLicence(start, end);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, INVALID_START_DATE_FORMAT_MSG),
            ValidationError.valueError(ANY_VRM, INVALID_END_DATE_FORMAT_MSG)
        );
      }

      @Test
      public void excelFormattedDatesReturnNoErrorForCsvSubmission() {
        // given
        String start = "01/01/2019";
        String end = "02/02/2019";
        VehicleDto licence = createLicenceWithTrigger(start, end, RegisterJobTrigger.CSV_FROM_S3);
 
        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).isEmpty();
      }

      @Test
      public void excelFormattedDatesReturnValueErrorForApiSubmission() {
        // given
        String start = "01/01/2019";
        String end = "02/02/2019";
        VehicleDto licence = createLicence(start, end);
 
        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, INVALID_START_DATE_FORMAT_MSG),
            ValidationError.valueError(ANY_VRM, INVALID_END_DATE_FORMAT_MSG)
        );
      }
    }
    @Nested
    class WithLineNumber {

      @Test
      public void shouldReturnValueErrorsWhenBothDatesHaveInvalidFormat() {
        // given
        int lineNumber = 74;
        String start = "2019-05-17-01";
        String end = "01/13/2019";
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, invalidStartDateFormatMsg(), lineNumber),
            ValidationError.valueError(ANY_VRM, invalidEndDateFormatMsg(), lineNumber)
        );
      }

      @Test
      public void excelFormattedDatesReturnNoErrorForCsvSubmission() {
        // given
        int lineNumber = 74;
        String start = "01/01/2019";
        String end = "02/02/2019";
        VehicleDto licence = createLicenceWithTriggerAndLineNumber(
            start, end, RegisterJobTrigger.CSV_FROM_S3, lineNumber);
 
        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).isEmpty();
      }

      @Test
      public void excelFormattedDatesReturnValueErrorForApiSubmission() {
        // given
        int lineNumber = 74;
        String start = "01/01/2019";
        String end = "02/02/2019";
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);
 
        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, INVALID_START_DATE_FORMAT_MSG ,lineNumber),
            ValidationError.valueError(ANY_VRM, INVALID_END_DATE_FORMAT_MSG, lineNumber)
        );
      }
    }
  }
  @Nested
  class DatesOrder {

    @Nested
    class WithoutLineNumber {

      @Test
      public void shouldReturnValueErrorsWhenBothDatesHaveInvalidFormat() {
        // given
        String start = "2019-05-17";
        String end = "2019-04-17";
        VehicleDto licence = createLicence(start, end);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, LicenceDatesValidator.INVALID_ORDER_DATE_MESSAGE)
        );
      }
    }
    @Nested
    class WithLineNumber {

      @Test
      public void shouldReturnValueErrorsWhenBothDatesHaveInvalidFormat() {
        // given
        int lineNumber = 75;
        String start = "2019-05-17";
        String end = "2019-04-17";
        VehicleDto licence = createLicenceWithLineNumber(start, end, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, LicenceDatesValidator.INVALID_ORDER_DATE_MESSAGE, lineNumber)
        );
      }

    }
  }
  @Nested
  class Value {
    
    @Test
    public void shouldReturnValueErrorWhenStartDateOver20YearsAgo() {
      // given
      int lineNumber = 72;
      String end = "2019-05-05";

      String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      String twentyYearsAgo = LocalDate.parse(today).minusYears(20).toString();

      VehicleDto licence = createLicenceWithLineNumber(twentyYearsAgo, end, lineNumber);

      // when
      List<ValidationError> validationErrors = validator.validate(licence);

      // then
      then(validationErrors).containsExactly(
          ValidationError.valueError(ANY_VRM, "Start date cannot be more than 20 years in the past", lineNumber));
    }

    @Test
    public void shouldReturnValueErrorWhenEndDateOver20YearsInFuture() {
      // given
      int lineNumber = 72;
      String start = "2019-05-05";

      String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      String twentyYearsAhead = LocalDate.parse(today).plusYears(20).toString();

      VehicleDto licence = createLicenceWithLineNumber(start, twentyYearsAhead, lineNumber);

      // when
      List<ValidationError> validationErrors = validator.validate(licence);

      // then
      then(validationErrors).containsExactly(
          ValidationError.valueError(ANY_VRM, "End date cannot be more than 20 years in the future", lineNumber)); 
    }
  }
  @Test
  public void shouldReturnEmptyListWhenDatesAreValid() {
    // given
    String start = "2019-05-17";
    String end = "2019-06-17";
    VehicleDto licence = createLicence(start, end);

    // when
    List<ValidationError> validationErrors = validator.validate(licence);

    // then
    then(validationErrors).isEmpty();
  }

  private String missingStartDateMsg() {
    return String.format(LicenceDatesValidator.MISSING_DATE_MESSAGE_TEMPLATE, "start");
  }

  private String missingEndDateMsg() {
    return String.format(LicenceDatesValidator.MISSING_DATE_MESSAGE_TEMPLATE, "end");
  }

  private String invalidStartDateFormatMsg() {
    return String.format(LicenceDatesValidator.INVALID_DATE_FORMAT_TEMPLATE, "start");
  }

  private String invalidEndDateFormatMsg() {
    return String.format(LicenceDatesValidator.INVALID_DATE_FORMAT_TEMPLATE, "end");
  }

  private VehicleDto createLicence(String start, String end) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .start(start)
        .end(end)
        .build();
  }

  private VehicleDto createLicenceWithLineNumber(String start, String end, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .start(start)
        .end(end)
        .lineNumber(lineNumber)
        .build();
  }

  private VehicleDto createLicenceWithTrigger(String start, String end, RegisterJobTrigger jobTrigger) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .start(start)
        .end(end)
        .registerJobTrigger(jobTrigger)
        .build();
  }

  private VehicleDto createLicenceWithTriggerAndLineNumber(String start, String end, RegisterJobTrigger jobTrigger, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .start(start)
        .end(end)
        .registerJobTrigger(jobTrigger)
        .lineNumber(lineNumber)
        .build();
  }
}