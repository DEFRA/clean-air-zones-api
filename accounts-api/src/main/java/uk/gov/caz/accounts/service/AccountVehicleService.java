package uk.gov.caz.accounts.service;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.google.common.base.Strings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.TravelDirection;
import uk.gov.caz.accounts.model.VehiclesWithAnyUndeterminedChargeabilityFlagData;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.AccountVehicleAlreadyExistsException;
import uk.gov.caz.accounts.service.exception.PageOutOfBoundsException;
import uk.gov.caz.accounts.service.exception.VrnNotFoundException;

/**
 * Service responsible for persisting and retrieving data about vehicles associated with accounts.
 */
@Service
@Slf4j
@AllArgsConstructor
public class AccountVehicleService {

  private final AccountVehicleRepository accountVehicleRepository;
  private final AccountRepository accountRepository;
  private final ChargeCalculationService chargeCalculationService;

  /**
   * Given an accountId and page parameters, this method returns a page of results with vehicles
   * associated with the given account.
   *
   * @param accountId the identifier of the account
   * @param query argument used to search by vrn
   * @param pageNumber the page that should be retrieved
   * @param pageSize the number of results in that page
   * @return the corresponding list of vehicles
   * @throws AccountNotFoundException exception raised in the event an account could not be
   *     reconciled.
   */
  public VehiclesWithAnyUndeterminedChargeabilityFlagData findVehiclesForAccount(UUID accountId,
      String query, int pageNumber, int pageSize, Boolean onlyChargeable) {
    verifyAccountPresence(accountId);
    Page<AccountVehicle> accountVehiclesPage;
    if (onlyChargeable) {
      accountVehiclesPage = fetchChargeableVehiclesPage(accountId, query, pageNumber, pageSize);
    } else {
      accountVehiclesPage = fetchVehiclesPage(accountId, query, pageNumber, pageSize);
    }

    boolean anyUndeterminedVehicles = accountContainsAnyUndeterminedCharges(accountId);

    return VehiclesWithAnyUndeterminedChargeabilityFlagData.builder()
        .vehicles(accountVehiclesPage)
        .anyUndeterminedVehicles(anyUndeterminedVehicles)
        .build();
  }

  /**
   * Fetches a page of vrns using cursor-based pagination.
   *
   * @param accountIdStr the ID of the account to fetch vrns for
   * @param pageSize the size of the page to return
   * @param vrn an optional vrn to use as a cursor
   * @param direction the direction in which to sort records
   * @return a list of vrns
   */
  public List<AccountVehicle> findVehiclesForAccountWithCursor(String accountIdStr, long pageSize,
      String vrn, TravelDirection direction, UUID chargeableCazId) {
    log.debug("fetching {} vrns with direction: {}", pageSize, direction);

    UUID accountId = verifyAccountPresence(accountIdStr);
    if (!StringUtils.hasText(vrn)) {
      List<UUID> accountVehicleIds = accountVehicleRepository
          .findPageByAccountIdAndCursorWithEmptyVrn(accountId, chargeableCazId, pageSize);
      return isEmpty(accountVehicleIds)
          ? emptyList()
          : accountVehicleRepository.findAllWithChargeabilityWith(accountVehicleIds);
    }
    verifyVrnPresence(accountId, vrn);

    if (direction == TravelDirection.NEXT) {
      List<UUID> accountVehicleIds = accountVehicleRepository
          .findPageByAccountIdAndCursorNextPage(accountId, vrn, chargeableCazId, pageSize);
      return isEmpty(accountVehicleIds)
          ? emptyList()
          : accountVehicleRepository
              .findAllWithChargeabilityByAccountVehicleIdsCursorNext(accountVehicleIds);
    }

    if (direction == TravelDirection.PREVIOUS) {
      List<UUID> accountVehicleIds = accountVehicleRepository
          .findPageByAccountIdAndCursorPrevPage(accountId, vrn, chargeableCazId, pageSize);
      return isEmpty(accountVehicleIds)
          ? emptyList()
          : accountVehicleRepository
              .findAllWithChargeabilityByAccountVehicleIdsCursorPrev(accountVehicleIds);
    }

    log.error("Sort direction invalid");
    throw new IllegalArgumentException("Sort direction invalid");
  }

  /**
   * Based on provided attributes, the method verifies if there is an account in the database with
   * provided {@code accountId} and then returns either the existing record with provided {@code
   * vrn} number or creates a new record. When new entity is created it will undergo process of
   * chargeability cache calculation, synchronously in process without lambda interaction.
   *
   * @param accountId identifies an {@link Account} for which the new vehicle is being added.
   * @param vrn identifies an {@link AccountVehicle} which is being added.
   * @return created or found {@link AccountVehicle}.
   * @throws AccountNotFoundException when {@link Account} with provided {@code accountId} does
   *     not exist.
   */
  @Transactional
  public AccountVehicle createAccountVehicle(String accountId, String vrn) {
    Optional<AccountVehicle> accountVehicle = getAccountVehicle(accountId, vrn);

    if (accountVehicle.isPresent()) {
      log.debug("AccountVehicle already exists");
      throw new AccountVehicleAlreadyExistsException();
    }

    AccountVehicle newAccountVehicle = AccountVehicle.builder()
        .accountId(UUID.fromString(accountId))
        .vrn(vrn)
        .build();

    newAccountVehicle = accountVehicleRepository.save(newAccountVehicle);
    chargeCalculationService.populateCacheForSingleVehicle(newAccountVehicle.getAccountVehicleId(),
        newAccountVehicle.getVrn());
    return newAccountVehicle;
  }

  /**
   * Method responsible for removing {@link AccountVehicle} from the database based on the provided
   * parameters.
   *
   * @param accountId identifies account for which the vehicle is going to be removed.
   * @param vrn identifies the account vehicle which is going to be removed.
   */
  @Transactional
  public void deleteAccountVehicle(String accountId, String vrn) {
    verifyAccountPresence(accountId);
    accountVehicleRepository.deleteByVrnAndAccountId(vrn, UUID.fromString(accountId));
  }

  /**
   * Method responsible for fetching a single {@link AccountVehicle} from the database based on the
   * provided parameters.
   *
   * @param accountId identifies account for which the vehicle is going to be queried.
   * @param vrn identifies the account vehicle which is going to be fetched.
   */
  public Optional<AccountVehicle> getAccountVehicle(String accountId, String vrn) {
    verifyAccountPresence(accountId);
    return accountVehicleRepository
        .findByAccountIdAndVrn(UUID.fromString(accountId), vrn);
  }

  /**
   * Method responsible for fetching a single {@link AccountVehicle} with chargeability from the
   * database based on the provided parameters.
   *
   * @param accountId identifies account for which the vehicle is going to be queried.
   * @param vrn identifies the account vehicle which is going to be fetched.
   */
  public Optional<AccountVehicle> getAccountVehicleWithChargeability(UUID accountId, String vrn) {
    verifyAccountPresence(accountId);
    return accountVehicleRepository
        .findByAccountIdAndVrnWithChargeability(accountId, vrn);
  }

  /**
   * Gets vehicles for the given account with their chargeability data.
   */
  private Page<AccountVehicle> fetchVehiclesPage(UUID accountId, String query, int pageNumber,
      int pageSize) {
    Page<AccountVehicle> accountVehicles;
    if (Strings.isNullOrEmpty(query)) {
      accountVehicles = accountVehicleRepository.findAllByAccountId(accountId,
          page(pageNumber, pageSize));
    } else {
      accountVehicles = accountVehicleRepository
          .findAllByAccountIdAndVrnContaining(accountId, query, page(pageNumber, pageSize));
    }

    verifyPageNumberBounds(pageNumber, accountVehicles);
    return enrichWithChargesData(accountVehicles);
  }


  /**
   * Gets chargeable vehicles for the given account with their chargeability data.
   */
  private Page<AccountVehicle> fetchChargeableVehiclesPage(UUID accountId,
      String query, int pageNumber, int pageSize) {
    Page<AccountVehicle> accountVehicles;
    if (Strings.isNullOrEmpty(query)) {
      accountVehicles = accountVehicleRepository
          .findAllWithChargeabilityFor(accountId, page(pageNumber, pageSize));
    } else {
      accountVehicles = accountVehicleRepository
          .findAllByVrnContainingWithChargeabilityFor(accountId, query, page(pageNumber, pageSize));
    }
    verifyPageNumberBounds(pageNumber, accountVehicles);
    return enrichWithChargesData(accountVehicles);
  }

  /**
   * Returns true if ANY vehicle of the given account has undetermined charge (null).
   */
  private boolean accountContainsAnyUndeterminedCharges(UUID accountId) {
    return accountVehicleRepository.countVehiclesWithUndeterminedChargeabilityFor(accountId) > 0;
  }

  /**
   * Verifies the value of {@code pageNumber}. Throws {@link PageOutOfBoundsException} if invalid.
   */
  private void verifyPageNumberBounds(int pageNumber, Page<AccountVehicle> accountVehicles) {
    // correct values for pageNumber: 0 <= pageNumber <= accountVehicles.getTotalPages() - 1
    // provided that any data is returned (accountVehicles.getTotalPages() > 0)
    if (accountVehicles.getTotalPages() > 0 && pageNumber > accountVehicles.getTotalPages() - 1) {
      throw new PageOutOfBoundsException();
    }
  }

  /**
   * Replaces provided {@code accountVehicles} with the ones that contains chargeability data. The
   * order of vehicles is preserved.
   */
  private Page<AccountVehicle> enrichWithChargesData(Page<AccountVehicle> accountVehicles) {
    Map<UUID, AccountVehicle> vehiclesWithChargesByVehicleId = getMatchingVehiclesWithCharges(
        accountVehicles);

    return accountVehicles.map(vehicleWithoutCharges ->
        vehiclesWithChargesByVehicleId.get(vehicleWithoutCharges.getAccountVehicleId()));
  }

  /**
   * For the provided {@code page} finds matching {@link AccountVehicle}s which include {@code
   * VehicleChargeability}.
   */
  private Map<UUID, AccountVehicle> getMatchingVehiclesWithCharges(Page<AccountVehicle> page) {
    List<UUID> accountVehicleIds = extractAccountVehicleIdsFrom(page);
    if (accountVehicleIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return accountVehicleRepository.findAllWithChargeability(accountVehicleIds)
        .stream()
        .collect(Collectors.toMap(AccountVehicle::getAccountVehicleId, Function.identity()));
  }

  /**
   * Creates {@link PageRequest} based on provided {@code pageNumber} and {@code pageSize}. The
   * created instance sorts vehicles by VRN (ascending).
   */
  private PageRequest page(int pageNumber, int pageSize) {
    return PageRequest.of(
        pageNumber,
        pageSize,
        Sort.sort(AccountVehicle.class).by(AccountVehicle::getVrn).ascending()
    );
  }

  /**
   * Extracts {@code account vehicle id}s from the passed {@code accountVehicles}.
   */
  private List<UUID> extractAccountVehicleIdsFrom(Page<AccountVehicle> accountVehicles) {
    return accountVehicles.get()
        .map(AccountVehicle::getAccountVehicleId)
        .collect(Collectors.toList());
  }

  /**
   * Verifies that the account with the given {@code accountId} exists by throwing {@link
   * AccountNotFoundException} if it does not.
   */
  private UUID verifyAccountPresence(String accountId) {
    return verifyAccountPresence(UUID.fromString(accountId));
  }

  /**
   * Verifies that the account with the given {@code accountId} exists by throwing {@link
   * AccountNotFoundException} if it does not.
   */
  private UUID verifyAccountPresence(UUID accountId) {
    // Assert presence of account under supplied ID, otherwise raise exception.
    Optional<Account> matchedAccount = accountRepository.findById(accountId);

    if (!matchedAccount.isPresent()) {
      log.debug("No matched account found.");
      throw new AccountNotFoundException("Account not found.");
    }
    return matchedAccount.get().getId();
  }

  /**
   * Verifies present of the given {@code vrn} in {@code accountId} fleet. Throws {@link
   * VrnNotFoundException} if vrn is not found.
   */
  private void verifyVrnPresence(UUID accountId, String vrn) {
    // Assert presence of VRN under given account
    Optional<AccountVehicle> matchedVehicle = accountVehicleRepository
        .findByAccountIdAndVrn(accountId, vrn);
    if (!matchedVehicle.isPresent()) {
      log.debug("No matched VRN found");
      throw new VrnNotFoundException("Vrn not found");
    }
  }
}
