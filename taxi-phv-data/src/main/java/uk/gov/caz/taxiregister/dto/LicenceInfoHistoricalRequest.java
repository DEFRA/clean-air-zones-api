package uk.gov.caz.taxiregister.dto;

import static uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalResult.DATE_FORMAT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.taxiregister.controller.exception.RequestParamsValidationException;

/**
 * Class that contains fields required to query licence info history.
 */
@Value
@Builder
public class LicenceInfoHistoricalRequest {

  /**
   * Page that will be retrieved.
   */
  long pageNumber;

  /**
   * The page size.
   */
  long pageSize;

  /**
   * ISO 8601 formatted date string indicating the modification date from.
   */
  LocalDateTime modifyDateFrom;

  /**
   * ISO 8601 formatted date string indicating the modification date To.
   */
  LocalDateTime modifyDateTo;

  /**
   * The vrm.
   */
  String vrm;

  /**
   * Convert http request propertise to {@link LicenceInfoHistoricalRequest} object.
   *
   * @param vrm to search.
   * @param queryStrings from http request.
   * @return {@link LicenceInfoHistoricalRequest}
   */
  public static LicenceInfoHistoricalRequest from(String vrm, Map<String, String> queryStrings) {
    try {
      return LicenceInfoHistoricalRequest
          .builder()
          .vrm(vrm)
          .modifyDateFrom(toLocalDateTime(queryStrings, "startDate", LocalTime.MIDNIGHT))
          .modifyDateTo(
              toLocalDateTime(queryStrings, "endDate", LocalTime.MIDNIGHT.minusNanos(1)))
          .pageSize(parsePositiveLong(queryStrings, "pageSize", 1))
          .pageNumber(parsePositiveLong(queryStrings, "pageNumber", 0))
          .build();
    } catch (Exception e) {
      throw new RequestParamsValidationException(
          "Unable to parse query string map reason:" + e.getMessage());
    }
  }

  protected static long parsePositiveLong(Map<String, String> queryStrings, String propertyName,
      long minValue) {
    long value = Long.parseLong(queryStrings.get(propertyName));
    if (value < minValue) {
      throw new IllegalArgumentException(propertyName + " cannot be negative");
    }
    return value;
  }

  /**
   * Convert string to {@link OffsetDateTime}.
   */
  protected static LocalDateTime toLocalDateTime(Map<String, String> queryStrings,
      String dateString, LocalTime localTime) {
    LocalDate date = LocalDate
        .parse(queryStrings.get(dateString), DateTimeFormatter.ofPattern(DATE_FORMAT));
    return LocalDateTime.of(date, localTime).atZone(ZoneId.of("Europe/London"))
        .withZoneSameInstant(ZoneId.of("GMT")).toLocalDateTime();
  }
}