package uk.gov.caz.accounts.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.DirectDebitMandateDto;
import uk.gov.caz.accounts.dto.DirectDebitMandateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesResponse;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateResponse;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.service.DirectDebitMandateService;
import uk.gov.caz.accounts.service.DirectDebitMandatesBulkUpdater;

@RestController
@RequiredArgsConstructor
public class DirectDebitMandateController implements DirectDebitMandateControllerApiSpec {

  public static final String DIRECT_DEBIT_MANDATE_PATH =
      "/v1/accounts/{accountId}/direct-debit-mandates";

  private final DirectDebitMandateService directDebitMandateService;
  private final DirectDebitMandatesBulkUpdater directDebitMandateBulkUpdater;

  @Override
  public ResponseEntity<DirectDebitMandateDto> createDirectDebitMandate(UUID accountId,
      DirectDebitMandateRequest directDebitMandateRequest) {
    directDebitMandateRequest.validate();

    DirectDebitMandate directDebitMandate = directDebitMandateService
        .create(accountId, directDebitMandateRequest);
    DirectDebitMandateDto response = DirectDebitMandateDto.from(directDebitMandate);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<DirectDebitMandatesResponse> getDirectDebitMandates(UUID accountId) {
    List<DirectDebitMandate> directDebitMandates = directDebitMandateService
        .findAllByAccountId(accountId);

    return ResponseEntity.ok(DirectDebitMandatesResponse.builder()
        .directDebitMandates(
            directDebitMandates.stream()
                .map(DirectDebitMandateDto::from)
                .collect(Collectors.toList()))
        .build());
  }

  @Override
  public DirectDebitMandatesUpdateResponse updateDirectDebitMandates(UUID accountId,
      DirectDebitMandatesUpdateRequest request) {
    request.validate();

    directDebitMandateBulkUpdater
        .updateStatuses(accountId, request.getDirectDebitMandates());

    return new DirectDebitMandatesUpdateResponse();
  }

  @Override
  public ResponseEntity<Void> deleteMandate(UUID accountId, UUID mandateId) {
    directDebitMandateService.delete(accountId, mandateId);
    return ResponseEntity.noContent().build();
  }
}
