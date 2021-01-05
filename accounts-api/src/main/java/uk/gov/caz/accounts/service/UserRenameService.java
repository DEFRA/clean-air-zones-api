package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;

/**
 * Service responsible for renaming a user.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserRenameService {

  private final IdentityProvider identityProvider;

  private final UserRepository userRepository;

  /**
   * Renames user in Identity Provider.
   * @param accountId account id of a user renamed
   * @param userId user id of a user renamed
   * @param newName new name of a user
   */
  public void updateUserName(UUID accountId, UUID userId, String newName) {
    Preconditions.checkNotNull(newName, "newName cannot be null");
    Preconditions.checkArgument(StringUtils.hasText(newName),
        "Standard user must have a non-empty name");

    UserEntity userEntity = userRepository
        .findByIdAndAccountId(userId, accountId)
        .filter(user -> !user.isRemoved())
        .filter(user -> !user.isOwner())
        .orElseThrow(() -> new AccountUserNotFoundException("User not found"));

    String email = identityProvider.getEmailByIdentityProviderId(
        userEntity.getIdentityProviderUserId());
    identityProvider.setUserName(email, newName);
  }
}