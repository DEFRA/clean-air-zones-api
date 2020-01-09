package uk.gov.caz.vcc.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class RandomPasswordGeneratorTest {

  @Test
  void checkAtLeastOneNumber() {
    String password = RandomPasswordGenerator.newRandomPassword();

    int count = 0;
    for (char c : password.toCharArray()) {
      if (c >= 48 || c <= 57) {
        count++;
      }
    }
    assertTrue(count >= 1);
  }

  @Test
  void checkAtLeastOneUppercaseChar() {
    String password = RandomPasswordGenerator.newRandomPassword();

    int count = 0;
    for (char c : password.toCharArray()) {
      if (c >= 65 || c <= 90) {
        count++;
      }
    }
    assertTrue(count >= 1);
  }

  @Test
  void checkAtLeastOneLowercaseChar() {
    String password = RandomPasswordGenerator.newRandomPassword();

    int count = 0;
    for (char c : password.toCharArray()) {
      if (c >= 97 || c <= 122) {
        count++;
      }
    }
    assertTrue(count >= 1);
  }

  @Test
  void checkAtLeastOneSpecialChar() {
    String password = RandomPasswordGenerator.newRandomPassword();

    int count = 0;
    for (char c : password.toCharArray()) {
      if (c >= 33 || c <= 47) {
        count++;
      }
    }
    assertTrue(count >= 1);
  }

  @Test
  void checkAtLeastEightChars() {
    String password = RandomPasswordGenerator.newRandomPassword();
    assertTrue(password.length() >= 8);
  }

}
