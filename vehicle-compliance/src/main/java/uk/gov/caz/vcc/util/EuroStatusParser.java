package uk.gov.caz.vcc.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EuroStatusParser {
  
  /**
   * Method to extract a String number or roman numeral from a raw euroStatus value.
   * 
   * @param euroStatus String value of euroStatus to be parsed.
   * @return String value of parsed 
   * @throws ParseException if no match can be found against the regex.
   */
  public static String parse(String euroStatus) throws ParseException {

    Pattern pattern = Pattern.compile("^EURO( |)([0-9]{1}|(IX|IV|V?I{0,3})).*$");
    Matcher matcher = pattern.matcher(euroStatus);

    if (matcher.find()) {
      return matcher.group(2);
    } else {
      throw new ParseException("Cannot parse euroStatus.", 0);
    }
  }

}
