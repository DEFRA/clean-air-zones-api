package uk.gov.caz.accounts.assertion;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.accounts.controller.AccountVehiclesController;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;

public class VehicleRetrievalAssertion {

  private String accountId;
  private String query;
  private String vrn;
  private String travelDirection;
  private String pageSize;
  private String chargeableCazId;
  private Boolean onlyChargeable;

  private ValidatableResponse vehicleResponse;
  private VehiclesResponseDto offsetVehicleResponseDto;
  private ChargeableVehiclesResponseDto cursorVehicleResponseDto;

  private static final String CORRELATION_ID = UUID.randomUUID().toString();

  public VehicleRetrievalAssertion forAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public VehicleRetrievalAssertion forQuery(String query) {
    this.query = query;
    return this;
  }

  public VehicleRetrievalAssertion forOnlyChargeable(Boolean onlyChargeable) {
    this.onlyChargeable = onlyChargeable;
    return this;
  }

  public VehicleRetrievalAssertion forVrn(String vrn) {
    this.vrn = vrn;
    return this;
  }

  public VehicleRetrievalAssertion forTravelDirection(String travelDirection) {
    this.travelDirection = travelDirection;
    return this;
  }

  public VehicleRetrievalAssertion forPageSize(String pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public VehicleRetrievalAssertion forChargeableCazId(String chargeableCazId) {
    this.chargeableCazId = chargeableCazId;
    return this;
  }

  public VehicleRetrievalAssertion whenRequestToRetrieveVehiclesIsMadeWithQueryStrings(
      String pageNumber,
      String pageSize) {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    this.vehicleResponse = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .pathParam("accountId", this.accountId)
        .queryParam("query", this.query)
        .queryParam("pageNumber", pageNumber)
        .queryParam("pageSize", pageSize)
        .queryParam("onlyChargeable", this.onlyChargeable)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get()
        .then();
    return this;
  }

  public VehicleRetrievalAssertion whenRequestToRetrieveVehiclesIsMadeWithOtherQueryStrings() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    this.vehicleResponse = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .pathParam("accountId", this.accountId)
        .queryParam("test", "123")
        .queryParam("test2", "abcd")
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get()
        .then();
    return this;
  }

  public VehicleRetrievalAssertion whenRequestToRetrieveVehiclesIsMade() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    this.vehicleResponse = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .pathParam("accountId", this.accountId)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get()
        .then();
    return this;
  }

  public VehicleRetrievalAssertion whenRequestForCursorBasedPaginationIsMade() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    this.vehicleResponse = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .pathParam("accountId", this.accountId)
        .queryParam("vrn", this.vrn)
        .queryParam("direction", travelDirection)
        .queryParam("pageSize", this.pageSize)
        .queryParam("chargeableCazId", this.chargeableCazId)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get("/sorted-page")
        .then();
    return this;
  }

  public VehicleRetrievalAssertion whenRequestForCursorBasedPaginationIsMadeWithoutDirection() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    this.vehicleResponse = RestAssured
        .given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .pathParam("accountId", this.accountId)
        .queryParam("vrn", this.vrn)
        .queryParam("pageSize", this.pageSize)
        .queryParam("chargeableCazId", this.chargeableCazId)
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .when()
        .get("/sorted-page")
        .then();
    return this;
  }

  public VehicleRetrievalAssertion then() {
    return this;
  }

  public VehicleRetrievalAssertion offsetResponseIsReturnedWithHttpOkStatusCode() {
    this.offsetVehicleResponseDto = vehicleResponse.statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .log().all()
        .extract()
        .as(VehiclesResponseDto.class);
    return this;
  }

  public VehicleRetrievalAssertion cursorResponseIsReturnedWithHttpOkStatusCode() {
    this.cursorVehicleResponseDto = vehicleResponse.statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .extract()
        .body()
        .as(ChargeableVehiclesResponseDto.class);
    return this;
  }

  public VehicleRetrievalAssertion responseIsReturnedWithHttpNotFoundStatusCode() {
    checkStatusCode(404);
    return this;
  }

  public VehicleRetrievalAssertion responseIsReturnedWithHttpBadRequestStatusCode() {
    checkStatusCode(400);
    return this;
  }

  public VehicleRetrievalAssertion andResponseContainsExpectedData(String vrns, String totalPages,
      Map<String, VehicleWithCharges> vrnToCharges) {
    checkVrnsInOffsetResponse(vrns);
    checkTotalPagesInResponse(totalPages);
    checkTotalVrnsInResponse();
    checkCharges(vrnToCharges);
    checkAnyUndeterminedVehiclesFlag(true);
    return this;
  }

  public VehicleRetrievalAssertion andResponseContainsExpectedVrns(String vrns, String totalPages) {
    checkVrnsInOffsetResponse(vrns);
    checkTotalPagesInResponse(totalPages);
    return this;
  }

  public VehicleRetrievalAssertion andResponseContainsAnyUndeterminedVehiclesFlagEqualToFalse() {
    checkAnyUndeterminedVehiclesFlag(false);
    return this;
  }

  public VehicleRetrievalAssertion andResponseContainsAnyUndeterminedVehiclesFlagEqualToTrue() {
    checkAnyUndeterminedVehiclesFlag(true);
    return this;
  }

  private void checkAnyUndeterminedVehiclesFlag(boolean expectedValue) {
    assertThat(offsetVehicleResponseDto.isAnyUndeterminedVehicles())
        .isEqualTo(expectedValue);
  }

  private void checkCharges(Map<String, VehicleWithCharges> vrnToCharges) {
    for (Entry<String, VehicleWithCharges> vrnWithCharges : vrnToCharges.entrySet()) {
      String vrn = vrnWithCharges.getKey();
      VehicleWithCharges expected = vrnWithCharges.getValue();
      VehicleWithCharges actual = offsetVehicleResponseDto.getVehicles()
          .stream()
          .filter(vehicleWithCharges -> vrn.equals(vehicleWithCharges.getVrn()))
          .findFirst()
          .orElseThrow(IllegalStateException::new);

      assertThat(actual.isExempt()).isEqualTo(expected.isExempt());
      assertThat(actual.isRetrofitted()).isEqualTo(expected.isRetrofitted());
      assertThat(actual.getVehicleType()).isEqualTo(expected.getVehicleType());
      assertThat(actual.getCachedCharges())
          .containsExactlyInAnyOrderElementsOf(expected.getCachedCharges());
    }
  }

  public VehicleRetrievalAssertion responseContainsCorrectCursorData(String vrns) {
    checkVrnsInCursorResponse(vrns);
    return this;
  }

  public void andOffsetResponseContainsEmptyListOfVrns() {
    assertEquals(0, this.offsetVehicleResponseDto.getVehicles().size());
  }

  public VehicleRetrievalAssertion andCursorResponseContainsEmptyListOfVrns() {
    assertEquals(0, this.cursorVehicleResponseDto.getTotalVehiclesCount());
    return this;
  }

  public VehicleRetrievalAssertion responseContainsQueryStringErrors(String pageNumber,
      String pageSize) {
    if (pageNumber == null) {
      checkResponseContainsError("pageNumber");
    }
    if (pageSize == null) {
      checkResponseContainsError("pageSize");
    }
    return this;
  }
  public VehicleRetrievalAssertion responseContainsQueryParamError() {
    checkResponseContainsError("query");
    return this;
  }

  private void checkResponseContainsError(String error) {
    this.vehicleResponse.body(containsString(error));
  }

  private void checkStatusCode(int statusCode) {
    vehicleResponse.statusCode(statusCode);
  }

  private void checkVrnsInCursorResponse(String vrns) {
    List<String> vrnList = Stream.of(vrns.split(",")).collect(toList());
    assertEquals(vrnList, this.cursorVehicleResponseDto
        .getVehicles()
        .stream()
        .map(VehicleWithCharges::getVrn)
        .collect(toList()));
  }

  private void checkVrnsInOffsetResponse(String vrns) {
    List<String> vrnList = Stream.of(vrns.split(";")).collect(toList());
    List<String> returnedVrns = offsetVehicleResponseDto.getVehicles().stream()
        .map(VehicleWithCharges::getVrn).collect(toList());

    assertEquals(vrnList, returnedVrns);
  }

  private void checkTotalPagesInResponse(String totalPages) {
    assertEquals(Integer.parseInt(totalPages), this.offsetVehicleResponseDto.getPageCount());
  }

  private void checkTotalVrnsInResponse() {
    assertEquals(6, this.offsetVehicleResponseDto.getTotalVehiclesCount());
  }

  public VehicleRetrievalAssertion hasTotalVehicleCount(int totalVehiclesCount) {
    assertThat(this.cursorVehicleResponseDto.getTotalVehiclesCount())
        .isEqualTo(totalVehiclesCount);
    return this;
  }

  public VehicleRetrievalAssertion andContainsExactlyFiveVehiclesInCorrectOrder() {
    ChargeableVehiclesResponseDto expectedChargeableVehiclesResponseDto = readJson(
        "five-vehicles.json");
    assertThat(cursorVehicleResponseDto.getVehicles())
        .containsExactly(
            expectedChargeableVehiclesResponseDto.getVehicles().get(0),
            expectedChargeableVehiclesResponseDto.getVehicles().get(1),
            expectedChargeableVehiclesResponseDto.getVehicles().get(2),
            expectedChargeableVehiclesResponseDto.getVehicles().get(3),
            expectedChargeableVehiclesResponseDto.getVehicles().get(4)
        );
    return this;
  }

  public VehicleRetrievalAssertion andContainsExactlyThreeVehiclesInCorrectOrder() {
    ChargeableVehiclesResponseDto expectedChargeableVehiclesResponseDto = readJson(
        "three-vehicles.json");
    assertThat(cursorVehicleResponseDto.getVehicles())
        .containsExactly(
            expectedChargeableVehiclesResponseDto.getVehicles().get(0),
            expectedChargeableVehiclesResponseDto.getVehicles().get(1),
            expectedChargeableVehiclesResponseDto.getVehicles().get(2)
        );
    return this;
  }

  public VehicleRetrievalAssertion andContainsExactlyFourVehiclesInCorrectOrder() {
    ChargeableVehiclesResponseDto expectedChargeableVehiclesResponseDto = readJson(
        "four-vehicles.json");
    assertThat(cursorVehicleResponseDto.getVehicles())
        .containsExactly(
            expectedChargeableVehiclesResponseDto.getVehicles().get(0),
            expectedChargeableVehiclesResponseDto.getVehicles().get(1),
            expectedChargeableVehiclesResponseDto.getVehicles().get(2),
            expectedChargeableVehiclesResponseDto.getVehicles().get(3)
        );
    return this;
  }

  public VehicleRetrievalAssertion andContainsExactlyOneVehicle() {
    ChargeableVehiclesResponseDto expectedChargeableVehiclesResponseDto = readJson(
        "one-vehicle.json");
    assertThat(cursorVehicleResponseDto.getVehicles())
        .containsExactly(
            expectedChargeableVehiclesResponseDto.getVehicles().get(0)
        );
    return this;
  }

  private ChargeableVehiclesResponseDto readJson(String filename) {
    try {
      String json = new String(Files
          .readAllBytes(ResourceUtils.getFile("classpath:data/json/cursor/" + filename).toPath()));
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(json, ChargeableVehiclesResponseDto.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
