package uk.gov.caz.accounts.util.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * A utility class responsible for calculating a SecretHash value required AWS Cognito User Pools
 * API.
 */
@UtilityClass
@Slf4j
public class SecretHashCalculator {

  private static final String HMAC_SHA_256_ALGORITHM = "HmacSHA256";

  /**
   * Computes SecretHash value for the passed {@code userPoolClientId}, {@code userPoolClientSecret}
   * and {@code userName} parameters. This method is copied from <a href="https://docs.aws.amazon.com/cognito/latest/developerguide/signing-up-users-in-your-app.html#cognito-user-pools-computing-secret-hash">AWS
   * documentation</a>.
   *
   * @param userPoolClientId An identifier of a client of the given user pool.
   * @param userPoolClientSecret A secret token of a client of the given user pool.
   * @param userName A username against which the SecretHash is computed.
   * @return A SecretHash for {@code userPoolClientId}, {@code userPoolClientSecret} and {@code
   *     userName} parameters.
   */
  public static String calculateSecretHash(String userPoolClientId, String userPoolClientSecret,
      String userName) {
    try {
      SecretKeySpec signingKey = new SecretKeySpec(
          userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
          HMAC_SHA_256_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_SHA_256_ALGORITHM);
      mac.init(signingKey);
      mac.update(userName.getBytes(StandardCharsets.UTF_8));
      byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(rawHmac);
    } catch (Exception e) {
      log.error("Error while calculating a SecretHash value for user pool client id: '{}'",
          userPoolClientId);
      throw new ApplicationRuntimeException("Error while calculating a SecretHash value");
    }
  }
}
