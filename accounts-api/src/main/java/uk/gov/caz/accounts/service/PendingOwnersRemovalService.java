package uk.gov.caz.accounts.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingOwnersRemovalService {

  private final UserRepository userRepository;
  private final UserRemovalService userRemovalService;

  /**
   * Method which removes pending owner users of the provided {@link Account}.
   *
   * @param accountId identifies {@link Account} from which the pending owner users are going to
   *     be removed.
   */
  @Transactional
  public void removeNonVerifiedOwnerUsers(UUID accountId) {
    List<UserEntity> usersToRemove = userRepository.findOwnersForAccount(accountId);

    List<UserRemovalStatus> removalStatuses = usersToRemove.stream()
        .map(userEntity -> userRemovalService.removeAnyUser(accountId, userEntity.getId())).collect(
            Collectors.toList());

    boolean hasAnyUnsuccessfulRemoval = removalStatuses.stream().anyMatch(
        userRemovalStatus -> !userRemovalStatus.equals(UserRemovalStatus.SUCCESSFULLY_DELETED));

    if (hasAnyUnsuccessfulRemoval) {
      log.warn("Error occurred while removing pending owner users from account {}", accountId);
    }
  }
}
