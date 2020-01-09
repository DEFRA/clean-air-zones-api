package uk.gov.caz.vcc.util;

import com.google.common.base.Preconditions;

import org.springframework.stereotype.Component;


/**
 * Utility component for converting roman numeral values to Integers.
 *
 */
@Component
public class RomanNumeralConverter {

  private static final String ROMAN_NUMERAL_REGEX = "(IX|IV|V?I{0,3})";

  /**
   * Checks if the candidateString is a roman numeral.
   * 
   * @param candidateString String value to be checked against regex
   * @return true if matches, else false
   */
  public boolean matchesRomanNumeralRegex(String candidateString) {
    return candidateString.matches(ROMAN_NUMERAL_REGEX);
  }

  /**
   * Converts any roman numeral to arabic value (i.e. an Integer). Handles
   * values from I to VIII (8 to 1).
   * 
   * @param romanNumeral String value to be converted
   * @return int of converted value
   */
  public int romanToArabic(String romanNumeral) {
    Preconditions.checkNotNull(romanNumeral);
  
    if (romanNumeral.length() <= 1) {
      switch (romanNumeral) {
        case "I":
          return 1;
        case "V":
          return 5;
        case "X":
          return 10;
        default:
          return 0;
      }
    } else {
      if (romanNumeral.startsWith("V")) {
        return 5 + romanToArabic(romanNumeral.substring(1));
      }
  
      if (romanNumeral.startsWith("X")) {
        return 10 + romanToArabic(romanNumeral.substring(1));
      }
  
      if (romanNumeral.startsWith("I")) {
        if (romanNumeral.substring(1).equals("V") || romanNumeral.substring(1).equals("X")) {
          return -1 + romanToArabic(romanNumeral.substring(1));
        } else {
          return 1 + romanToArabic(romanNumeral.substring(1));
        }
      }
    }

    throw new NumberFormatException(
        "Could not convert roman numeral to an integer: " + romanNumeral);
  }
}
