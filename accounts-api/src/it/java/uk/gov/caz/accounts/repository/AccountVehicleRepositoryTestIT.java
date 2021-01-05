package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehicleChargeability;

@IntegrationTest
@Transactional
public class AccountVehicleRepositoryTestIT {

  @Autowired
  private AccountVehicleRepository accountVehicleRepository;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private VehicleChargeabilityRepository vehicleChargeabilityRepository;

  private UUID createdAccountId;

  @BeforeEach
  public void setupAll() {
    Account account = accountRepository.save(Account.builder()
        .name(RandomStringUtils.random(5)).build());
    createdAccountId = account.getId();
  }

  @AfterEach
  public void teardown() {
    accountVehicleRepository.deleteInBulkByAccountId(createdAccountId);
    jdbcTemplate.execute(String.format(
        "DELETE FROM caz_account.t_account WHERE account_id = '%s'",
        createdAccountId.toString()));
  }

  @Test
  @Sql(scripts = {"classpath:data/sql/create-vehicles-and-chargeability-cache-data.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldClearVehicleChargeabilityTableWhenRemovingFromAccountVehicleTable() {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");

    // when
    JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "caz_account.t_account_vehicle",
        "account_id = '" + accountId.toString() + "'"
            + " and vrn in ('VRN1', 'VRN2')");
    Set<String> vrnsForThisAccount = accountVehicleRepository
        .findAllByAccountId(accountId, Pageable.unpaged()).get().map(AccountVehicle::getVrn)
        .collect(Collectors.toSet());
    Iterable<VehicleChargeability> vehicleChargeabilities = vehicleChargeabilityRepository
        .findAll();

    // then
    assertThat(vrnsForThisAccount).containsExactlyInAnyOrder("VRN3");
    assertThat(vehicleChargeabilities).hasSize(1);
  }

  @Test
  @Sql(scripts = {"classpath:data/sql/create-vehicles-and-chargeability-cache-data.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldNotClearAccountVehicleTableWhenRemovingFromVehicleChargeabilityTable() {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");

    // when
    vehicleChargeabilityRepository.deleteFromVehicleChargeability(getAccountVehicleIds());
    Set<String> vrnsForThisAccount = accountVehicleRepository
        .findAllByAccountId(accountId, Pageable.unpaged()).get().map(AccountVehicle::getVrn)
        .collect(Collectors.toSet());
    Iterable<VehicleChargeability> vehicleChargeabilities = vehicleChargeabilityRepository
        .findAll();

    // then
    assertThat(vrnsForThisAccount).containsExactlyInAnyOrder("VRN1", "VRN2", "VRN3");
    assertThat(vehicleChargeabilities).hasSize(0);
  }

  @Test
  @Sql(scripts = {"classpath:data/sql/create-vehicles-with-the-same-vrn-for-two-accounts.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldRemoveAccountVehicleOnlyForGivenAccount() {
    // given
    UUID firstAccountId = UUID.fromString("2b424742-686f-4e63-b810-771c30da940c");
    UUID secondAccountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");

    // when
    accountVehicleRepository.deleteByVrnAndAccountId("VRN1", firstAccountId);
    accountVehicleRepository.deleteByVrnAndAccountId("VRN2", secondAccountId);
    Set<String> vrnsForFirstAccount = getVrnsForGivenAccount(firstAccountId);
    Set<String> vrnsForSecondAccount = getVrnsForGivenAccount(secondAccountId);

    // then
    assertThat(vrnsForFirstAccount).containsExactlyInAnyOrder("VRN2", "VRN3");
    assertThat(vrnsForSecondAccount).containsExactlyInAnyOrder("VRN1", "VRN3");
  }

  @Test
  @Sql(scripts = {"classpath:data/sql/create-vehicles-and-chargeability-cache-data.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldGetAllVehiclesForGivenAccountWithAscOrderByVrn() {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");

    // when
    List<AccountVehicle> accountVehicles = accountVehicleRepository
        .findByAccountIdWithChargeabilityAndOrderByVrnAsc(accountId);

    // then
    assertThat(accountVehicles).hasSize(3);
    assertThat(accountVehicles.get(0).getVrn()).isEqualTo("VRN1");
    assertThat(accountVehicles.get(1).getVrn()).isEqualTo("VRN2");
    assertThat(accountVehicles.get(2).getVrn()).isEqualTo("VRN3");
  }

  private Set<String> getVrnsForGivenAccount(UUID accountId) {
    return accountVehicleRepository
        .findAllByAccountId(accountId, Pageable.unpaged()).get()
        .map(AccountVehicle::getVrn)
        .collect(Collectors.toSet());
  }

  private HashSet<UUID> getAccountVehicleIds() {
    return Sets.newHashSet(
        UUID.fromString("ccbc6bea-4b0f-45ec-bbf2-021451823441"),
        UUID.fromString("13c52f66-fdc1-43e2-b6af-a67d04987776"),
        UUID.fromString("6d3c83de-2c89-443c-be17-662bdde3841a")
    );
  }
}