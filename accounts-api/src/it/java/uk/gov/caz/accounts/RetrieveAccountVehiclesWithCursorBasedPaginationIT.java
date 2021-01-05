package uk.gov.caz.accounts;

import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.accounts.annotation.FullyRunningIntegrationTest;
import uk.gov.caz.accounts.assertion.VehicleRetrievalAssertion;
import uk.gov.caz.accounts.controller.AccountVehiclesController;
import uk.gov.caz.accounts.model.TravelDirection;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/create-account-vehicle-data-cursor.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-account-vehicle-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class RetrieveAccountVehiclesWithCursorBasedPaginationIT {

  @LocalServerPort
  int randomServerPort;

  private static final String ACCOUNT_ID = "1f30838f-69ee-4486-95b4-7dfcd5c6c67c";
  private static final String ACCOUNT_ID_NO_VEHICLES = "3de21da7-86fc-4ccc-bab3-130f3a10e380";
  private static final String CAZ_ID = "4a09097a-2175-4146-b7df-90dd9e58ac5c";

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }

  @Test
  public void shouldReturn200OkAndCorrectResponseWhenValidAscendingRequestSupplied() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forVrn("ABC456")
        .forTravelDirection(TravelDirection.NEXT.name())
        .forPageSize("3")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMade()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .responseContainsCorrectCursorData("BX92 CNE,CAS123,DKC789")
        .hasTotalVehicleCount(3)
        .andContainsExactlyThreeVehiclesInCorrectOrder();
  }

  @Test
  public void shouldReturn200OkAndCorrectResponseWhenValidDescendingRequestSupplied() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forVrn("PO44 BCN")
        .forTravelDirection(TravelDirection.PREVIOUS.name())
        .forPageSize("5")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMade()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .responseContainsCorrectCursorData("KQ93NFL,DKC789,CAS123,BX92 CNE,ABC456")
        .hasTotalVehicleCount(5)
        .andContainsExactlyFiveVehiclesInCorrectOrder();
  }

  @Test
  public void shouldReturn200OkAndCorrectResponseWhenRequestWithoutDirectionSupplied() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forVrn("BX92 CNE")
        .forPageSize("1")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .responseContainsCorrectCursorData("CAS123")
        .hasTotalVehicleCount(1)
        .andContainsExactlyOneVehicle();
  }

  @Test
  public void shouldReturn200OkAndCorrectResponseWhenRequestWithoutVrnOrDirectionSupplied() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("4")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .responseContainsCorrectCursorData("ABC456,BX92 CNE,CAS123,DKC789")
        .hasTotalVehicleCount(4)
        .andContainsExactlyFourVehiclesInCorrectOrder();
  }

  @Test
  public void shouldReturn200OkAndEmptyArrayWhenNoVrnsCanBeFound() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID_NO_VEHICLES)
        .forPageSize("10")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .andCursorResponseContainsEmptyListOfVrns()
        .hasTotalVehicleCount(0);
  }

  @Test
  public void shouldReturn200OkAndEmptyArrayWhenNoVrnsCanBeFoundWithNextDirection() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forVrn("PO44 BCN")
        .forPageSize("10")
        .forChargeableCazId(CAZ_ID)
        .forTravelDirection(TravelDirection.NEXT.name())
        .whenRequestForCursorBasedPaginationIsMade()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .andCursorResponseContainsEmptyListOfVrns()
        .hasTotalVehicleCount(0);
  }

  @Test
  public void shouldReturn200OkAndEmptyArrayWhenNoVrnsCanBeFoundWithPrevDirection() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forVrn("ABC456")
        .forPageSize("10")
        .forChargeableCazId(CAZ_ID)
        .forTravelDirection(TravelDirection.PREVIOUS.name())
        .whenRequestForCursorBasedPaginationIsMade()
        .cursorResponseIsReturnedWithHttpOkStatusCode()
        .andCursorResponseContainsEmptyListOfVrns()
        .hasTotalVehicleCount(0);
  }

  @Test
  public void shouldReturn400BadRequestWhenPageSizeIsZero() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("0")
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .responseIsReturnedWithHttpBadRequestStatusCode();
  }

  @Test
  public void shouldReturn400BadRequestWhenPageSizeNotSupplied() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .responseIsReturnedWithHttpBadRequestStatusCode();
  }

  @ParameterizedTest
  @CsvSource(
      {
          "-1,next,ABC456,4a09097a-2175-4146-b7df-90dd9e58ac5c",
          "1,test,ABC456,4a09097a-2175-4146-b7df-90dd9e58ac5c",
          "10,previous,,4a09097a-2175-4146-b7df-90dd9e58ac5c",
          "10, next, ABC456,non-uuid-value"
      })
  public void shouldReturn400BadRequestWhenQueryStringsAreInvalid(
      String pageSize, String direction, String vrn, String cazId) {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forPageSize(pageSize)
        .forTravelDirection(direction)
        .forVrn(vrn)
        .forChargeableCazId(cazId)
        .whenRequestForCursorBasedPaginationIsMade()
        .responseIsReturnedWithHttpBadRequestStatusCode();
  }

  @Test
  public void shouldReturn400BadRequestWhenChargeableCazIdIsMissing() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .responseIsReturnedWithHttpBadRequestStatusCode();
  }

  @Test
  public void shouldReturn404NotFoundWhenAccountIdCannotBeFound() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(UUID.randomUUID().toString())
        .forPageSize("10")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .responseIsReturnedWithHttpNotFoundStatusCode();
  }

  @Test
  public void shouldReturn404NotFoundWhenVrnCannotBeFound() {
    RestAssured.basePath = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
    givenVehicleRetrieval()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forVrn("TESTVRN")
        .forChargeableCazId(CAZ_ID)
        .whenRequestForCursorBasedPaginationIsMadeWithoutDirection()
        .responseIsReturnedWithHttpNotFoundStatusCode();
  }

  private VehicleRetrievalAssertion givenVehicleRetrieval() {
    return new VehicleRetrievalAssertion();
  }

}
