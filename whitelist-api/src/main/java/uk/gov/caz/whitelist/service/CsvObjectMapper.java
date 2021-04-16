package uk.gov.caz.whitelist.service;

import static uk.gov.caz.whitelist.model.CategoryType.fromCategory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto.WhitelistedVehicleDtoBuilder;
import uk.gov.caz.whitelist.model.CategoryType;
import uk.gov.caz.whitelist.model.CsvParseResult;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.service.exception.CsvBlankRowException;
import uk.gov.caz.whitelist.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.whitelist.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.whitelist.service.exception.CsvMaxLineLengthExceededException;
import uk.gov.caz.whitelist.service.validation.CsvAwareValidationMessageModifier;

/**
 * A class that provides methods to map an {@link InputStream} of a CSV data to a {@link Set} of
 * {@link WhitelistVehicle}.
 */
@Component
@Slf4j
public class CsvObjectMapper {

  private static final String LINE_TOO_LONG_MESSAGE_TEMPLATE = "Line is too long (actual value: "
      + "%d, allowed value: %d).";
  private static final String LINE_INVALID_FORMAT_MESSAGE = "Line contains invalid "
      + "character(s), is empty or has trailing comma character.";
  private static final String LINE_INVALID_FIELDS_COUNT_MESSAGE_TEMPLATE = "Line contains "
      + "invalid number of fields (actual value: %d, allowable value: %d).";
  private static final String BLANK_ROW_MESSAGE = "Blank row. Please remove or add data.";

  private final CsvAwareValidationMessageModifier messageModifier;

  public CsvObjectMapper(CsvAwareValidationMessageModifier messageModifier) {
    this.messageModifier = messageModifier;
  }

  private static boolean notNullOrEmpty(String vrn) {
    return !Strings.isNullOrEmpty(vrn);
  }

  /**
   * Reads data from {@code inputStream} and maps it to a {@link CsvParseResult}. The {@code
   * inputStream} *MUST* contain data in CSV format. The {@code inputStream} *MUST* be closed by the
   * client code to avoid memory leaks.
   *
   * @param inputStream A stream which contains data in CSV format
   * @return {@link CsvParseResult}
   */
  public CsvParseResult read(InputStream inputStream) throws IOException {
    ImmutableList.Builder<WhitelistedVehicleDto> vehiclesBuilder = ImmutableList.builder();
    LinkedList<ValidationError> errors = Lists.newLinkedList();
    CSVReader reader = createReader(inputStream);

    String[] fields;
    int lineNo = 1;
    while ((fields = readLine(reader, errors, lineNo)) != null) {
      if (fields.length == 0) {
        log.trace("Validation error on line {}, skipping it", lineNo);
      } else {
        WhitelistedVehicleDto whitelistedVehicleDto = createWhitelistedVehicle(fields, lineNo);
        vehiclesBuilder.add(whitelistedVehicleDto);
        log.debug("Whitelisted vehicle read: {}", whitelistedVehicleDto);
      }
      lineNo += 1;
    }
    ImmutableList<WhitelistedVehicleDto> vehicles = vehiclesBuilder.build();

    addTrailingRowErrorInfoIfApplicable(reader, errors, lineNo - 1);
    addDuplicatedVrnsErrorIfApplicable(vehicles, errors);
    logParsingEndReason();

    return new CsvParseResult(vehicles, Collections.unmodifiableList(errors));
  }

  private void addDuplicatedVrnsErrorIfApplicable(List<WhitelistedVehicleDto> vehicles,
      List<ValidationError> errors) {
    long numberOfUniqueVrns = vehicles
        .stream()
        .map(WhitelistedVehicleDto::getVrn)
        .filter(CsvObjectMapper::notNullOrEmpty)
        .distinct()
        .count();

    if (countWithoutEmptyVrns(vehicles) != numberOfUniqueVrns) {
      errors.add(
          ValidationError.valueError("There are multiple entries with the same VRN")
      );
    }
  }

  private long countWithoutEmptyVrns(List<WhitelistedVehicleDto> vehicles) {
    return vehicles
        .stream()
        .map(WhitelistedVehicleDto::getVrn)
        .filter(CsvObjectMapper::notNullOrEmpty)
        .count();
  }

  private void logParsingEndReason() {
    log.info("Finished parsing the input file: reached EOF");
  }

  private void addTrailingRowErrorInfoIfApplicable(CSVReader reader,
      LinkedList<ValidationError> errors, int numberOfLines) throws IOException {
    if (hasNotParsedWholeFile(reader) || errors.isEmpty()) {
      log.trace("Skipped adding the info about the trailing row (errors size: {})", errors.size());
      return;
    }
    ValidationError lastError = errors.pollLast(); // lastError != null
    ValidationError lastErrorReplacement = computeLastErrorReplacement(numberOfLines, lastError);
    errors.add(lastErrorReplacement);
  }

  private boolean hasNotParsedWholeFile(CSVReader reader) throws IOException {
    try {
      return reader.peek() != null;
    } catch (IOException ioException) {
      throw ioException;
    } catch (Exception e) {
      //This exception is thrown by CsvWhitelistedVehicleParse.
      //As long as it is business-logic specific we do not care whether next line is valid or not.
      //Unfortunately, "reader.peek()" does execute read + validation which we do not need here
      //hence this catch clause.
      return true;
    }
  }

  private ValidationError computeLastErrorReplacement(int numberOfLines,
      ValidationError lastError) {
    if (validationErrorHappenedOnLastLine(numberOfLines, lastError)) {
      String newDetail = messageModifier.addTrailingRowInfoSuffix(lastError.getRawDetail());
      return ValidationError.copyWithNewDetail(lastError, newDetail);
    }
    return lastError;
  }

  private boolean validationErrorHappenedOnLastLine(int numberOfLines, ValidationError lastError) {
    // assertion: lastError != null
    return lastError.getLineNumber().map(lineNo -> lineNo == numberOfLines).orElse(Boolean.FALSE);
  }

  private CSVReader createReader(InputStream inputStream) {
    CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new InputStreamReader(inputStream));
    csvReaderBuilder.withCSVParser(new CsvWhitelistedVehicleParser(new CSVParser()));
    return csvReaderBuilder.build();
  }

  private WhitelistedVehicleDto createWhitelistedVehicle(String[] fields, int lineNo) {
    WhitelistedVehicleDtoBuilder vehicleDtoBuilder = WhitelistedVehicleDto.builder()
        .vrn(Optional.ofNullable(StringUtils.deleteWhitespace(fields[0]))
            .map(String::toUpperCase).orElse(null))
        .reason(Strings.emptyToNull(trimString(fields[2])))
        .manufacturer(Optional.ofNullable(trimString(Strings.emptyToNull(fields[3]))))
        .action(trimString(fields[4]))
        .lineNumber(lineNo);
    Optional<CategoryType> categoryType = fromCategory(trimString(fields[1]));
    categoryType.ifPresent(type -> {
      vehicleDtoBuilder.category(type.getCategory());
      vehicleDtoBuilder.exempt(type.isExempt());
      vehicleDtoBuilder.compliant(type.isCompliant());
    });
    return vehicleDtoBuilder.build();
  }

  /// Helper method to trim any whitelist from non-null VRNs
  private static String trimString(String vrn) {
    if (vrn != null) {
      vrn = vrn.trim();
    }
    return vrn;
  }

  private ValidationError createInvalidFieldsCountError(int lineNo,
      CsvInvalidFieldsCountException e) {
    return ValidationError.valueError(
        modifyErrorMessage(lineNo, invalidFieldsCountErrorDetail(e)), lineNo);
  }

  private ValidationError createMaximumLineLengthExceededError(int lineNo,
      CsvMaxLineLengthExceededException e) {
    return ValidationError.valueError(
        modifyErrorMessage(lineNo, maximumLineLengthErrorDetail(e)), lineNo);
  }

  private ValidationError createParseValidationError(int lineNo) {
    return ValidationError.valueError(
        modifyErrorMessage(lineNo, LINE_INVALID_FORMAT_MESSAGE), lineNo);
  }

  private ValidationError createBlankRowError(int lineNo) {
    return ValidationError.valueError(modifyErrorMessage(lineNo, BLANK_ROW_MESSAGE), lineNo);
  }

  private String invalidFieldsCountErrorDetail(CsvInvalidFieldsCountException e) {
    return String.format(LINE_INVALID_FIELDS_COUNT_MESSAGE_TEMPLATE, e.getFieldsCount(),
        CsvWhitelistedVehicleParser.EXPECTED_FIELDS_CNT);
  }

  private String maximumLineLengthErrorDetail(CsvMaxLineLengthExceededException e) {
    return String.format(LINE_TOO_LONG_MESSAGE_TEMPLATE, e.getLineLength(),
        CsvWhitelistedVehicleParser.MAX_LINE_LENGTH);
  }

  /**
   * Reads line and returns null if end of the stream, empty array if validation error, non-empty
   * array on success.
   */
  private String[] readLine(CSVReader reader, LinkedList<ValidationError> errors, int lineNo)
      throws IOException {
    try {
      return reader.readNext();
    } catch (CsvInvalidFieldsCountException e) {
      log.debug("Invalid number of fields detected: {}", e.getMessage());
      errors.add(createInvalidFieldsCountError(lineNo, e));
    } catch (CsvBlankRowException e) {
      log.debug("Encountered a blank row on line {}", lineNo);
      errors.add(createBlankRowError(lineNo));
    } catch (CsvMaxLineLengthExceededException e) {
      log.debug("Maximum line length exceeded: {}", e.getMessage());
      errors.add(createMaximumLineLengthExceededError(lineNo, e));
    } catch (CsvInvalidCharacterParseException e) {
      log.debug("Error while parsing line {}: {}", lineNo, e.getMessage());
      errors.add(createParseValidationError(lineNo));
    }
    return new String[0];
  }

  private String modifyErrorMessage(int lineNumber, String message) {
    return messageModifier.addHeaderRowInfoSuffix(message, lineNumber);
  }
}
