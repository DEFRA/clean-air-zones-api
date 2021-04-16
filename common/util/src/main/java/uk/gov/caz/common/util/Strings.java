package uk.gov.caz.common.util;

import com.google.common.base.Preconditions;
import java.util.UUID;

/**
 * Static utility methods related to {@link String} instances.
 */
public class Strings {

  private Strings() {
    // utility class
  }

  private static final int DEFAULT_TO_REVEAL_LENGTH = 3;

  /**
   * Masks the passed {@code input} with '*' and reveals its first three characters.
   * If the input is shorter than three characters, all characters are masked.
   *
   * @param input String which is to be masked.
   * @return A masked string with its first three characters revealed.
   * @throws NullPointerException if {@code input} is null.
   */
  public static String mask(String input) {
    Preconditions.checkNotNull(input, "'input' cannot be null");

    int toReveal = computeToReveal(input);
    int toMask = input.length() - toReveal;
    return input.substring(0, toReveal) + com.google.common.base.Strings.repeat("*", toMask);
  }

  /**
   * Checks whether the passed {@code potentialUuid} is a valid {@link UUID}.
   * @param potentialUuid String which is to be checked.
   * @return true if {@code potentialUuid} is a valid UUID, false otherwise.
   */
  public static boolean isValidUuid(String potentialUuid) {
    Preconditions.checkNotNull(potentialUuid, "'potentialUuid' cannot be null");
    try {
      // see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8159339
      return UUID.fromString(potentialUuid).toString().equals(potentialUuid);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Computes how many characters should be revealed.
   */
  private static int computeToReveal(String input) {
    int inputLength = input.length();
    if (inputLength <= DEFAULT_TO_REVEAL_LENGTH) {
      return 0;
    }
    return Math.min(DEFAULT_TO_REVEAL_LENGTH, inputLength);
  }
}
