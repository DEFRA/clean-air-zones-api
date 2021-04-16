package uk.gov.caz.tariff.service;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

/**
 * Contains helper methods common to Tariff-API repositories.
 */
@UtilityClass
public class RepositoryUtils {

  /**
   * Given results set, takes active_charge_start_time value and returns String representation.
   *
   * @param rs Result set.
   * @return String representation of date.
   * @throws SQLException if date was malformed.
   */
  public static String safelyGetActiveChargeStartDate(ResultSet rs) throws SQLException {
    String chargeStartTime = "";
    Date chargeStartTimeDt = rs.getDate("active_charge_start_time");
    if (chargeStartTimeDt != null) {
      LocalDate localDate = chargeStartTimeDt.toLocalDate();
      chargeStartTime = localDate.format(DateTimeFormatter.ISO_DATE);
    }
    return chargeStartTime;
  }
}
