package uk.gov.caz.accounts.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class to hash strings using the SHA-256 algorithm.
 */
@Component
@Slf4j
public class Sha2Hasher {

  /**
   * Hashes a string using the SHA-256 algorithm.
   *
   * @param input the string to be hashed
   * @return the hash of that string
   */
  @SneakyThrows
  public static String sha256Hash(String input) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] encodedHash = digest.digest(
        input.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(encodedHash);
  }

  private static String bytesToHex(byte[] hash) {
    StringBuffer hexString = new StringBuffer();
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
