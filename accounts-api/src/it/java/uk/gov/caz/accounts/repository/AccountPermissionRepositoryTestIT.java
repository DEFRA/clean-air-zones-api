package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.Permission;

@IntegrationTest
public class AccountPermissionRepositoryTestIT {

  @Autowired
  private AccountPermissionRepository accountPermissionRepository;

  @Test
  void shouldReturnAllValidPermissions() {
    Iterable<AccountPermission> permissions = accountPermissionRepository.findAll();

    assertThat(permissions).hasSize(5);

    Set<Permission> permissionNames = StreamSupport.stream(permissions.spliterator(), false)
        .map(AccountPermission::getName)
        .collect(Collectors.toSet());
    assertThat(permissionNames).containsExactlyInAnyOrder(Permission.MAKE_PAYMENTS,
        Permission.MANAGE_MANDATES, Permission.MANAGE_USERS, Permission.MANAGE_VEHICLES,
        Permission.VIEW_PAYMENTS);
  }

}
