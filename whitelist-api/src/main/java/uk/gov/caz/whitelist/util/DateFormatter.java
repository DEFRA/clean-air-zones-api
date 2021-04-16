package uk.gov.caz.whitelist.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class DateFormatter {

  private static final DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Method to parser {@link LocalDateTime} into {@link String}.
   *
   * @param localDateTime date
   * @return formatted {@link String} yyyy-MM-dd HH:mm:ss
   */
  public String parse(LocalDateTime localDateTime) {
    return localDateTime.format(formatter);
  }
}