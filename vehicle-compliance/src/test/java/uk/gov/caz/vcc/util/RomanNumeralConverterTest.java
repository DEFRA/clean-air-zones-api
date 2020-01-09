package uk.gov.caz.vcc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RomanNumeralConverterTest {

  RomanNumeralConverter romanNumeralConverter = new RomanNumeralConverter();

  @Test
  void matchesNumeralsUpToEight() {
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("I"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("II"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("III"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("IV"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("V"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("VI"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("VII"));
    assertTrue(romanNumeralConverter.matchesRomanNumeralRegex("VIII"));
  }

  @Test
  void doesNotMatchArabicNumbers() {
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("1"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("2"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("3"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("4"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("5"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("6"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("7"));
    assertFalse(romanNumeralConverter.matchesRomanNumeralRegex("8"));
  }

  @Test
  void returnsCorrectInteger() {
    assertEquals(0, romanNumeralConverter.romanToArabic(""));
    assertEquals(1, romanNumeralConverter.romanToArabic("I"));
    assertEquals(2, romanNumeralConverter.romanToArabic("II"));
    assertEquals(3, romanNumeralConverter.romanToArabic("III"));
    assertEquals(4, romanNumeralConverter.romanToArabic("IV"));
    assertEquals(5, romanNumeralConverter.romanToArabic("V"));
    assertEquals(6, romanNumeralConverter.romanToArabic("VI"));
    assertEquals(7, romanNumeralConverter.romanToArabic("VII"));
    assertEquals(8, romanNumeralConverter.romanToArabic("VIII"));
    assertEquals(9, romanNumeralConverter.romanToArabic("IX"));
    assertEquals(10, romanNumeralConverter.romanToArabic("X"));
    assertEquals(11, romanNumeralConverter.romanToArabic("XI"));
    assertThrows(NumberFormatException.class, () -> romanNumeralConverter.romanToArabic("ZZ"));
  }

}