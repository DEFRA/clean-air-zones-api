package uk.gov.caz.vcc.util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomPasswordGenerator {

  /**
   * Generate a random integer between two and four to vary the number of
   * characters of each type that get generated.
   * 
   * @return an integer between two and four (inclusive)
   */
  private static int randomInt() {
    return ThreadLocalRandom.current().nextInt(2, 5);
  }

  /**
   * Generates a random password with upper case and lower case letters, special
   * characters and numbers.
   * 
   * @return
   */
  public static String newRandomPassword() {
    // generate random characters for the new password
    String upperCaseLetters =
        RandomStringUtils.random(randomInt(), 65, 90, true, true);
    String lowerCaseLetters =
        RandomStringUtils.random(randomInt(), 97, 122, true, true);
    String numbers = RandomStringUtils.randomNumeric(randomInt());
    String specialChar =
        RandomStringUtils.random(randomInt(), 33, 47, false, false);
    String totalChars = RandomStringUtils.randomAlphanumeric(randomInt());

    // combine and shuffle these characters and rebuild the string
    String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
        .concat(numbers).concat(specialChar).concat(totalChars);
    List<Character> pwdChars = combinedChars.chars().mapToObj(c -> (char) c)
        .collect(Collectors.toList());
    Collections.shuffle(pwdChars);

    return pwdChars.stream().collect(StringBuilder::new,
        StringBuilder::append, StringBuilder::append).toString();
  }
}
