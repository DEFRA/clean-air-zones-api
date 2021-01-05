package uk.gov.caz.accounts.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.service.exception.AccountUserCodeHashCreationException;

/**
 * Component used to encrypt provided code.
 */
@Component
@Slf4j
public class TokenToHashConverter {

  /**
   * Shared, thread safe instance of Base64 encoder used to encode binary hashes into clean
   * Strings.
   */
  private static final Encoder base64Encoder = Base64.getEncoder();

  /**
   * Method to encrypt provided code with hardcoded salt.
   *
   * @param code UUID which need to be encrypted.
   * @return encrypted code as String.
   * @throws AccountUserCodeHashCreationException when internal exceptions were catch.
   */
  public String convert(UUID code) {
    byte[] hash = createHashForCode(code.toString());

    return base64Encoder.encodeToString(hash);
  }

  /**
   * As it is UUID we don't need any random salt.
   *
   * @return hardcoded salt.
   */
  private byte[] salt() {
    return "ANY_SALT".getBytes();
  }

  /**
   * Creates hash for provided code using PBKDF2.
   *
   * @param code which need to be encoded
   * @return hashed code as array of bytes.
   * @throws AccountUserCodeHashCreationException when internal exceptions were catch.
   */
  private byte[] createHashForCode(String code) {
    try {
      KeySpec spec = new PBEKeySpec(code.toCharArray(), salt(), 65536, 128);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      return factory.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      log.error(ex.getMessage());
      throw new AccountUserCodeHashCreationException(ex.getMessage());
    }
  }
}
