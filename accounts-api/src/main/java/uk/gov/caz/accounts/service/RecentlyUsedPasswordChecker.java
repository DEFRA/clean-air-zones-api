package uk.gov.caz.accounts.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.service.exception.PasswordRecentlyUsedException;
import uk.gov.caz.accounts.util.Sha2Hasher;

/**
 * Utility used to check if password was used recently.
 */
@Service
@AllArgsConstructor
public class RecentlyUsedPasswordChecker {

  private final IdentityProvider identityProvider;

  /**
   * Checks if password was not used recently.
   *
   * @param password password to be checked
   * @param email email of user
   */
  public void checkIfPasswordWasNotUsedRecently(String password, String email) {
    String hashedPassword = Sha2Hasher.sha256Hash(password);
    List<String> previousPasswords = identityProvider.previousPasswordsForUser(email);
    if (previousPasswords.contains(hashedPassword)) {
      throw new PasswordRecentlyUsedException("Password has been used recently");
    }
  }
}
