package uk.gov.caz.taxiregister.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.LocalDate;
import javax.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.taxiregister.dto.reporting.ActiveLicencesAuditInfo;
import uk.gov.caz.taxiregister.dto.reporting.LicensingAuthoritiesAuditInfo;
import uk.gov.caz.taxiregister.dto.validation.VrmValidator;
import uk.gov.caz.taxiregister.dto.validation.constraint.NotFuture;

@RequestMapping(
    produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE}
)
@Validated
public interface ReportingControllerApiSpec {

  /**
   * Returns licensing authorities (names) of active licences for a given VRM and date.
   *
   * @param vrm vrm Vehicle registration mark.
   * @param date The date against which the check is performed. Must be a date from the past or
   *     today. Today is used if null.
   * @return {@link LicensingAuthoritiesAuditInfo} which contains a list of licensing authorities.
   */
  @ApiOperation(
      value = "${swagger.operations.reporting.active-license-authority-names.description}",
      response = LicensingAuthoritiesAuditInfo.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @GetMapping(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH)
  LicensingAuthoritiesAuditInfo getLicensingAuthoritiesOfActiveLicencesForVrmOn(
      @PathVariable @Pattern(regexp = VrmValidator.REGEX) String vrm,
      @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) @NotFuture
          LocalDate date);

  /**
   * Returns VRMs which had at least one active licence for a given licensing authority on a given
   * date.
   *
   * @param licensingAuthorityId The identifier of the licensing authority.
   * @param date The date against which the check is performed. Must be a date from the past or
   *     today. Today is used if null.
   * @return {@link ActiveLicencesAuditInfo} which contains a list of VRMs.
   */
  @ApiOperation(
      value = "${swagger.operations.reporting.vrms-with-active-licences.description}",
      response = ActiveLicencesAuditInfo.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @GetMapping(ReportingController.ACTIVE_LICENCES_AUDIT_PATH)
  ActiveLicencesAuditInfo getActiveLicencesForLicensingAuthorityOn(
      @PathVariable int licensingAuthorityId,
      @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) @NotFuture
          LocalDate date);


}
