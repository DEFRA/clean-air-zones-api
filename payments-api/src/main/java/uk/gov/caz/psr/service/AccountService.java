package uk.gov.caz.psr.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.psr.dto.accounts.UserDetailsResponse;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.AccountNotFoundException;
import uk.gov.caz.psr.service.exception.AccountVehiclesBadRequest;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;
import uk.gov.caz.psr.service.exception.UserNotFoundException;

/**
 * Service to interact with the Accounts Service.
 */
@AllArgsConstructor
@Service
public class AccountService {

  private static final String ADMINISTRATOR = "Administrator";
  private static final String DELETED_USER = "Deleted user";
  private final AccountsRepository accountsRepository;
  private final GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;

  /**
   * Fetches a list of vehicles from the Accounts Service.
   *
   * @param accountId the account whose vehicles should be returned
   * @param pageNumber number of the page
   * @param pageSize the size of the list to be returned
   * @param cazId Clean Air Zone Id
   * @param query part of the VRN used to partial search
   * @return a response from the Accounts API
   * @throws AccountNotFoundException if API returns status 404
   */
  public VehiclesResponseDto getAccountVehicles(UUID accountId, int pageNumber,
      int pageSize, UUID cazId, String query) {
    Response<VehiclesResponseDto> accountsResponse = accountsRepository
        .getAccountChargeableVehiclesSync(accountId, pageNumber, pageSize, cazId, query);
    if (accountsResponse.isSuccessful()) {
      return accountsResponse.body();
    } else {
      if (accountsResponse.code() == 404) {
        throw new AccountNotFoundException();
      } else if (accountsResponse.code() == 400) {
        throw new AccountVehiclesBadRequest();
      } else {
        throw new ExternalServiceCallException();
      }
    }
  }

  /**
   * Gets entrant payments for a list of VRNs in a 13 day payment window.
   *
   * @param vrns map of VRNs against entrant payments
   * @param cleanAirZoneId an identitier for the clean air zone
   */
  public Map<String, List<EntrantPayment>> getPaidEntrantPayments(
      List<String> vrns, UUID cleanAirZoneId) {
    return getPaidEntrantPaymentsService.getResults(
        new HashSet<>(vrns),
        LocalDate.now().minusDays(6),
        LocalDate.now().plusDays(6),
        cleanAirZoneId);
  }

  /**
   * Method for retrieving a payer name.
   *
   * @param userId {@link UUID}
   * @return {@link String} When a given user is an account owner, payerName should be
   *     “Administrator”. If a user has been deleted it should be set to “Deleted user” otherwise
   *     should return name.
   */
  public String getPayerName(UUID userId) {
    UserDetailsResponse userDetails = getUserDetails(userId);
    if (userDetails.isOwner()) {
      return ADMINISTRATOR;
    }
    if (userDetails.isRemoved()) {
      return DELETED_USER;
    }
    return userDetails.getName();
  }

  private UserDetailsResponse getUserDetails(UUID userId) {
    Response<UserDetailsResponse> userDetailsResponse = accountsRepository
        .getUserDetailsSync(userId);
    if (userDetailsResponse.isSuccessful()) {
      return userDetailsResponse.body();
    } else {
      if (userDetailsResponse.code() == 404) {
        throw new UserNotFoundException();
      } else {
        throw new ExternalServiceCallException();
      }
    }
  }
}