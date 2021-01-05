package uk.gov.caz.accounts.service.emailnotifications;

import java.util.Optional;
import lombok.Value;
import uk.gov.caz.accounts.model.Account;

/**
 * Helper class to carry more information when an email is sent.
 */
@Value(staticConstructor = "of")
public class EmailContext {

  private static final EmailContext EMPTY = EmailContext.of(null);

  Account account;

  public Optional<Account> getAccount() {
    return Optional.ofNullable(account);
  }

  public static EmailContext empty() {
    return EMPTY;
  }
}
