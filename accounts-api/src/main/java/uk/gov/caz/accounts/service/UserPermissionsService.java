package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.AccountUserPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountPermissionRepository;
import uk.gov.caz.accounts.repository.AccountUserPermissionRepository;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;

/**
 * Sets permissions for a given user.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPermissionsService {

  private final AccountPermissionRepository accountPermissionRepository;
  private final AccountUserPermissionRepository accountUserPermissionRepository;
  private final UserRepository userRepository;

  /**
   * Sets permissions for a user with {@code userId} and account {@code accountId}.
   *
   * @param accountId User's account identifier.
   * @param userId User's identifier.
   * @param newPermissions A collection of permissions that is to be set for the given user.
   * @throws AccountUserNotFoundException if the user is not found.
   * @throws NullPointerException if {@code newPermissions} is null.
   */
  @Transactional
  public void updatePermissions(UUID accountId, UUID userId, Set<Permission> newPermissions) {
    Preconditions.checkNotNull(newPermissions, "newPermissions cannot be null");

    UserEntity userEntity = userRepository
        .findByIdAndAccountId(userId, accountId)
        .filter(user -> user.getIdentityProviderUserId() != null)
        .orElseThrow(() -> new AccountUserNotFoundException("User not found"));
    List<AccountPermission> currentPermissions =
        (userEntity.getAccountPermissions() != null) ? userEntity.getAccountPermissions()
            : Collections.emptyList();

    Set<AccountUserPermission> toDelete = computeToDelete(userId, newPermissions,
        currentPermissions);
    Set<AccountUserPermission> toAdd = computeToAdd(userId, newPermissions,
        currentPermissions);
    accountUserPermissionRepository.deleteAll(toDelete);
    accountUserPermissionRepository.saveAll(toAdd);
    log.info("Successfully set permissions {} for user '{}'", newPermissions, userId);
  }

  /**
   * Computes a set of permissions that needs to be saved for the user.
   */
  private Set<AccountUserPermission> computeToAdd(UUID accountUserId,
      Set<Permission> newPermissions, List<AccountPermission> currentPermissions) {
    Map<Permission, AccountPermission> allPermissionsByName = StreamSupport
        .stream(accountPermissionRepository.findAll().spliterator(), false)
        .collect(Collectors.toMap(AccountPermission::getName, Function.identity()));

    Set<Permission> currentPermissionNames = currentPermissions.stream()
        .map(AccountPermission::getName)
        .collect(Collectors.toSet());
    return Sets.difference(newPermissions, currentPermissionNames)
        .stream()
        .map(permission -> createUserPermission(accountUserId, allPermissionsByName, permission))
        .collect(Collectors.toSet());
  }

  /**
   * Computes a set of permissions that needs to be deleted for the user.
   */
  private Set<AccountUserPermission> computeToDelete(UUID accountUserId,
      Set<Permission> newPermissions, List<AccountPermission> currentPermissions) {
    return currentPermissions.stream()
        .filter(permission -> !newPermissions.contains(permission.getName()))
        .map(permission -> createUserPermission(accountUserId, permission.getId()))
        .collect(Collectors.toSet());
  }

  /**
   * Helper method that creates an instance of {@link AccountUserPermission}.
   */
  private AccountUserPermission createUserPermission(UUID accountUserId,
      Map<Permission, AccountPermission> allPermissions, Permission permission) {
    Long accountPermissionId = findAccountPermissionId(allPermissions, permission);
    return createUserPermission(accountUserId, accountPermissionId);
  }

  /**
   * Helper method that creates an instance of {@link AccountUserPermission}.
   */
  private AccountUserPermission createUserPermission(UUID accountUserId, Long id) {
    return AccountUserPermission.builder()
        .accountUserId(accountUserId)
        .accountPermissionId(id)
        .build();
  }

  /**
   * Finds {@link AccountPermission} by the passed {@code permission} in {@code allPermissions}.
   */
  private Long findAccountPermissionId(Map<Permission, AccountPermission> allPermissions,
      Permission permission) {
    return allPermissions.get(permission).getId();
  }
}
