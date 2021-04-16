package uk.gov.caz.vcc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class Sha2HasherTest {

  @ParameterizedTest
  @CsvSource({"Test 123,0e55b9efccb8afe8230e22616ae629037425bbf955aa4e35a7c1fdb2d61aca6c",
      "London Bridge,916921afa7bef95e83698fe6bc32b2af2f5e5aa07a26fd7def41e90ae240ff6d",
      "DwmqU!FH4AHL9^pyFs8xtU&ZAQ!S3W9wMm?Lm7=pmGZ2&X5FqY,"
      + "6629447bf229b8345b4eac40849450ef0f2b44e5b14432a7adc44fd27f55d2b9"})
  public void canSuccessfullySha256HashStrings(String input, String output) {
    String result = Sha2Hasher.sha256Hash(input);
    assertEquals(result, output);
  }

}
