package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.controller.DirectDebitMandateController.DIRECT_DEBIT_MANDATE_PATH;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.DirectDebitMandateDto;
import uk.gov.caz.accounts.dto.DirectDebitMandateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesResponse;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateResponse;

@RequestMapping(
    value = DIRECT_DEBIT_MANDATE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface DirectDebitMandateControllerApiSpec {

  /**
   * Endpoint specification that handles the Direct Debit Mandate creation.
   *
   * @return {@link DirectDebitMandateDto} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.direct-debit-mandate.create.description}",
      response = DirectDebitMandateDto.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 404, message = "Account was not found"),
      @ApiResponse(code = 422, message = "Direct debit mandate with id already exists"),
      @ApiResponse(code = 201, message = "Direct Debit Mandate created")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          paramType = "path"
      )
  })
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<DirectDebitMandateDto> createDirectDebitMandate(
      @PathVariable("accountId") UUID accountId,
      @RequestBody DirectDebitMandateRequest directDebitMandateRequest
  );

  /**
   * Endpoint specification that handles getting the Direct Debit Mandates assigned to the account.
   *
   * @return {@link DirectDebitMandateDto} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.direct-debit-mandate.get.description}",
      response = DirectDebitMandateDto.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 200, message = "Direct Debit Mandates found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          paramType = "path"
      )
  })
  @GetMapping
  ResponseEntity<DirectDebitMandatesResponse> getDirectDebitMandates(
      @PathVariable("accountId") UUID accountId
  );

  /**
   * Endpoint specification that handles multiple Direct Debit Mandates update.
   *
   * @return {@link DirectDebitMandatesUpdateResponse} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.direct-debit-mandate.update.description}",
      response = DirectDebitMandatesUpdateResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 404, message = "Account was not found"),
      @ApiResponse(code = 200, message = "DirectDebitMandates were successfully updated")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          paramType = "path"
      )
  })
  @PatchMapping
  DirectDebitMandatesUpdateResponse updateDirectDebitMandates(
      @PathVariable("accountId") UUID accountId,
      @RequestBody DirectDebitMandatesUpdateRequest directDebitMandatesUpdateRequest
  );

  /**
   * Endpoint specification that returns result of removing specified Direct Debit Mandate.
   *
   * @param accountId string representing accountId.
   * @param mandateId string representing directDebitMandateId assigned to given accountId.
   */
  @ApiOperation(value = "${swagger.operations.direct-debit-mandate.delete.description}")
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 400, message = "DirectDebitMandate does not belongs to provided Account"),
      @ApiResponse(code = 404, message = "Account was not found"),
      @ApiResponse(code = 204, message = "DirectDebitMandate Deleted")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          paramType = "path"
      )
  })
  @DeleteMapping("/{mandateId}")
  ResponseEntity deleteMandate(
      @PathVariable("accountId") UUID accountId,
      @PathVariable("mandateId") UUID mandateId
  );
}
