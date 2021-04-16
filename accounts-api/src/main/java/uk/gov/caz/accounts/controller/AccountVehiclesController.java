package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.util.VehiclesResponseDtoConverter.toVehicleResponse;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.controller.util.QueryStringValidator;
import uk.gov.caz.accounts.dto.AccountVehicleRequest;
import uk.gov.caz.accounts.dto.AccountVehicleResponse;
import uk.gov.caz.accounts.dto.CsvExportResponse;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehiclesWithAnyUndeterminedChargeabilityFlagData;
import uk.gov.caz.accounts.service.AccountVehicleService;
import uk.gov.caz.accounts.service.generatecsv.CsvFileSupervisor;
import uk.gov.caz.accounts.util.VehiclesResponseDtoConverter;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountVehiclesController implements AccountVehiclesControllerApiSpec {

  public static final String ACCOUNT_VEHICLES_PATH = "/v1/accounts/{accountId}/vehicles";
  public static final String CSV_EXPORTS = "/csv-exports";
  public static final String SINGLE_VEHICLE_PATH_SEGMENT = "/{vrn}";

  private static final String PAGE_SIZE_QUERY_PARAM = "pageSize";
  private static final String PAGE_NUMBER_QUERY_PARAM = "pageNumber";
  private static final String ONLY_CHARGEABLE_QUERY_PARAM = "onlyChargeable";
  private static final String ONLY_DETERMINED = "onlyDetermined";
  private static final String CAZ_ID = "cazId";
  private static final String QUERY_PARAM = "query";

  private final AccountVehicleService accountVehicleService;
  private final QueryStringValidator queryStringValidator;
  private final CsvFileSupervisor csvFileSupervisor;
  private static final int MAX_VRN_LENGTH = 15;

  @Value("${csv-export-bucket}")
  private String bucketName;

  @Override
  public ResponseEntity<AccountVehicleResponse> createVehicle(String accountId,
      AccountVehicleRequest request) {
    request.validate();
    AccountVehicle accountVehicle = accountVehicleService
        .createAccountVehicle(accountId, request.getVrn());

    AccountVehicleResponse response = AccountVehicleResponse.from(accountVehicle);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<VehiclesResponseDto> getAccountVehicleVrnsWithOffset(
      UUID accountId, Map<String, String> map) {
    log.info("Getting vehicle vrns for account id '{}'", accountId);
    // validate the necessary query parameters have been correctly supplied
    List<String> requiredParams = Arrays
        .asList(PAGE_NUMBER_QUERY_PARAM, PAGE_SIZE_QUERY_PARAM, QUERY_PARAM);
    queryStringValidator.validateRequest(map, requiredParams);

    int pageNumber = Integer.parseInt(map.get(PAGE_NUMBER_QUERY_PARAM));
    int pageSize = Integer.parseInt(map.get(PAGE_SIZE_QUERY_PARAM));
    Boolean onlyChargeable = Boolean.valueOf(map.get(ONLY_CHARGEABLE_QUERY_PARAM));
    Boolean onlyDetermined = Boolean.valueOf(map.get(ONLY_DETERMINED));
    String query = map.get(QUERY_PARAM);
    Optional<String> cazId = Optional.ofNullable(map.get(CAZ_ID));

    VehiclesWithAnyUndeterminedChargeabilityFlagData result = map.containsKey(CAZ_ID)
        ? accountVehicleService
        .findVehiclesForAccountInCaz(accountId, query, UUID.fromString(cazId.get()), pageNumber,
            pageSize, onlyChargeable, onlyDetermined)
        : accountVehicleService
            .findVehiclesForAccount(accountId, query, pageNumber, pageSize, onlyChargeable,
                onlyDetermined);

    logResultsInfo(result);

    return ResponseEntity.ok(VehiclesResponseDtoConverter.toVehiclesResponseDto(result, cazId));
  }

  @Override
  public ResponseEntity<Void> deleteVehicle(String accountId, String vrn) {
    validateVrnLength(vrn);
    accountVehicleService.deleteAccountVehicle(accountId, vrn);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<VehicleWithCharges> getVehicle(UUID accountId, String vrn) {
    validateVrnLength(vrn);

    return accountVehicleService.getAccountVehicleWithChargeability(accountId, vrn)
        .map(accountVehicle -> ResponseEntity
            .ok(toVehicleResponse(accountVehicle, Optional.empty())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<CsvExportResponse> csvExport(UUID accountId) {
    URL url = csvFileSupervisor.uploadCsvFileAndGetPresignedUrl(accountId);
    return ResponseEntity
        .ok(CsvExportResponse
            .builder()
            .fileUrl(url)
            .bucketName(bucketName).build());
  }

  /**
   * Logs information about how many vehicles are on the fetched page and the page number.
   */
  private void logResultsInfo(VehiclesWithAnyUndeterminedChargeabilityFlagData result) {
    Page<AccountVehicle> page = result.getVehicles();
    log.info("Returning {} vehicles on page {}", page.getNumberOfElements(), page.getNumber());
  }

  /**
   * Check whether VRN is shorter than specified maximum number of characters.
   *
   * @param vrn VRN to check.
   * @throws InvalidRequestPayloadException when VRN has invalid length.
   */
  private void validateVrnLength(String vrn) {
    if (vrn.length() > MAX_VRN_LENGTH) {
      throw new InvalidRequestPayloadException("VRN cannot be longer than 15 characters");
    }
  }
}