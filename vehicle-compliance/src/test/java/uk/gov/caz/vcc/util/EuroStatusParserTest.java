package uk.gov.caz.vcc.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.Assert.assertEquals;

public class EuroStatusParserTest {

  @MethodSource("streamEuroStatusTestCases")
  @ParameterizedTest
  public void parsesValuesOk(String raw, String expected) throws ParseException {
    assertEquals(expected, EuroStatusParser.parse(raw));
  }

  @Test
  public void raisesParseExeptionIfFails() {
    assertThrows(ParseException.class, () -> {EuroStatusParser.parse("euro5");});
  }

  private static Stream<Arguments> streamEuroStatusTestCases() {
    return Stream.of(
        Arguments.of("EURO 6", "6"),
        Arguments.of("EURO VI", "VI"),
        Arguments.of("EURO 6  AD", "6"),
        Arguments.of("EURO 6  AG", "6"),
        Arguments.of("EURO 6  DG", "6"),
        Arguments.of("EURO 6 (AD", "6"),
        Arguments.of("EURO 6 (AG", "6"),
        Arguments.of("EURO 6 (W)", "6"),
        Arguments.of("EURO 6 AD", "6"),
        Arguments.of("EURO 6 AF", "6"),
        Arguments.of("EURO 6 AG", "6"),
        Arguments.of("EURO 6 AH", "6"),
        Arguments.of("EURO 6 AJ", "6"),
        Arguments.of("EURO 6 BG", "6"),
        Arguments.of("EURO 6 CH", "6"),
        Arguments.of("EURO 6 CI", "6"),
        Arguments.of("EURO 6 DG", "6"),
        Arguments.of("EURO 6 P", "6"),
        Arguments.of("EURO 6 W", "6"),
        Arguments.of("EURO 6 X", "6"),
        Arguments.of("EURO 6 Y", "6"),
        Arguments.of("EURO 6 ZC", "6"),
        Arguments.of("EURO 6 ZF", "6"),
        Arguments.of("EURO 6(AD)", "6"),
        Arguments.of("EURO 6(AG)", "6"),
        Arguments.of("EURO 6(ZD)", "6"),
        Arguments.of("EURO 6AD", "6"),
        Arguments.of("EURO 6AG", "6"),
        Arguments.of("EURO 6B", "6"),
        Arguments.of("EURO 6BG", "6"),
        Arguments.of("EURO 6D", "6"),
        Arguments.of("EURO 6D TEMP", "6"),
        Arguments.of("EURO 6DG", "6"),
        Arguments.of("EURO 6D-TEMP", "6"),
        Arguments.of("EURO 6W", "6"),
        Arguments.of("EURO 6X", "6"),
        Arguments.of("EURO 6Y", "6"),
        Arguments.of("EURO VI C", "VI"),
        Arguments.of("EURO VI-C", "VI"),
        Arguments.of("EURO4", "4"),
        Arguments.of("EURO6", "6"),
        Arguments.of("EURO6AG", "6"),
        Arguments.of("EURO6B Y", "6"),
        Arguments.of("EURO6C", "6"),
        Arguments.of("EURO6D-TEMP", "6"),
        Arguments.of("EURO6Y", "6"),
        Arguments.of("EUROIV", "IV")
    );
  }
  
}
