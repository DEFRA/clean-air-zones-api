package uk.gov.caz.accounts;

import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.accounts.annotation.FullyRunningIntegrationTest;
import uk.gov.caz.accounts.assertion.VehicleRetrievalAssertion;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/create-account-vehicle-data-offset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-account-vehicle-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class RetrieveAccountVehiclesWithOffsetPaginationIT {

  @LocalServerPort
  int randomServerPort;

  private static final String VALID_ACCOUNT_ID = "1f30838f-69ee-4486-95b4-7dfcd5c6c67c";

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }

  @ParameterizedTest
  @MethodSource("accountVehiclesProvider")
  public void retrieveAllVehiclesAssociatedWithAccountTest(String pageNumber, String pageSize,
      String totalPages, String vrns, Map<String, VehicleWithCharges> vrnToCharges) {
    givenVehicleRetrieval()
        .forAccountId(VALID_ACCOUNT_ID)
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings(pageNumber, pageSize)
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsExpectedData(vrns, totalPages, vrnToCharges);
  }

  @Test
  public void retrieveAllChargeableAssociatedWithAccount() {
    givenVehicleRetrieval()
        .forAccountId("1dfe3e6d-363e-4b21-acca-0968ba764d46")
        .forOnlyChargeable(Boolean.TRUE)
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsExpectedVrns("EST121;EST123;EST124;EST125;EST126", "1")
        .andResponseContainsAnyUndeterminedVehiclesFlagEqualToTrue();
  }

  @Test
  public void retrieveAllChargeableAssociatedWithAccountAndQuery() {
    givenVehicleRetrieval()
        .forAccountId("1dfe3e6d-363e-4b21-acca-0968ba764d46")
        .forOnlyChargeable(Boolean.TRUE)
        .forQuery("24")
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsExpectedVrns("EST124", "1")
        .andResponseContainsAnyUndeterminedVehiclesFlagEqualToTrue();
  }

  @Test
  public void retrieveAllAssociatedWithAccountAndQuery() {
    givenVehicleRetrieval()
        .forAccountId("1dfe3e6d-363e-4b21-acca-0968ba764d46")
        .forQuery("24")
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsExpectedVrns("EST124", "1")
        .andResponseContainsAnyUndeterminedVehiclesFlagEqualToTrue();
  }

  @Test
  public void retrieveAppropriatePageOfChargeableAssociatedWithAccount() {
    givenVehicleRetrieval()
        .forAccountId("1dfe3e6d-363e-4b21-acca-0968ba764d46")
        .forOnlyChargeable(Boolean.TRUE)
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("2", "1")
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsExpectedVrns("EST124", "5")
        .andResponseContainsAnyUndeterminedVehiclesFlagEqualToTrue();
  }

  @Test
  public void retrieveAllVehiclesAssociatedWithAccountContainingVehiclesWithChargeabilityData() {
    givenVehicleRetrieval()
        .forAccountId("e6cf9e24-31b1-45c9-9149-5fc866424386")
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andResponseContainsAnyUndeterminedVehiclesFlagEqualToFalse();
  }

  @ParameterizedTest
  @CsvSource({"2,", ",", ",3", "1, 10", "-1,10", "0,-1", "test,number","0,0"})
  public void return400IfInvalidQueryStringsSupplied(
      String pageNumber, String pageSize) {
    givenVehicleRetrieval()
        .forAccountId(VALID_ACCOUNT_ID)
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings(pageNumber, pageSize)
        .then()
        .responseIsReturnedWithHttpBadRequestStatusCode()
        .responseContainsQueryStringErrors(pageNumber, pageSize);
  }

  @ParameterizedTest
  @ValueSource(strings = {"@", ",", "*", "%", "-1", "te$t", "Q;"})
  public void return400IfInvalidQueryParamSupplied(String query) {
    givenVehicleRetrieval()
        .forAccountId(VALID_ACCOUNT_ID)
        .forQuery(query)
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .responseIsReturnedWithHttpBadRequestStatusCode()
        .responseContainsQueryParamError();
  }

  @Test
  public void returnEmptyListIfNoVehiclesAssociatedWithAccount() {
    // Note the account ID here is drawn from  create-account-vehicle-data-offset.sql
    givenVehicleRetrieval()
        .forAccountId("3de21da7-86fc-4ccc-bab3-130f3a10e380")
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .offsetResponseIsReturnedWithHttpOkStatusCode()
        .andOffsetResponseContainsEmptyListOfVrns();
  }

  @Test
  public void return404IfAccountNotFound() {
    givenVehicleRetrieval()
        .forAccountId("a20713dd-48c4-4816-9785-8888bd4bf855")
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .responseIsReturnedWithHttpNotFoundStatusCode();
  }

  @Test
  public void return400IfAccountIdNotWellFormatted() {
    givenVehicleRetrieval()
        .forAccountId("a20")
        .whenRequestToRetrieveVehiclesIsMadeWithQueryStrings("0", "10")
        .then()
        .responseIsReturnedWithHttpBadRequestStatusCode();
  }

  private VehicleRetrievalAssertion givenVehicleRetrieval() {
    return new VehicleRetrievalAssertion();
  }

  static Stream<Arguments> accountVehiclesProvider() {
    return Stream.of(
        Arguments.arguments("0", "1", "6", "ABC456",
            Collections.singletonMap(
                "ABC456",
                VehicleWithCharges.builder()
                    .cachedCharges(
                        Arrays.asList(
                            VehicleCharge.builder()
                                .tariffCode("t-1")
                                .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                                .charge(BigDecimal.valueOf(2.56))
                                .build(),
                            VehicleCharge.builder()
                                .tariffCode("t-2")
                                .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                                .charge(BigDecimal.valueOf(4.89))
                                .build()
                        )
                    )
                    .vrn("ABC456")
                    .exempt(false)
                    .retrofitted(false)
                    .vehicleType("Van")
                    .build()
            )
        ),
        Arguments.arguments("0", "3", "2", "ABC456;BX92 CNE;CAS123",
            ImmutableMap.builder()
                .put(
                    "ABC456",
                    VehicleWithCharges.builder()
                        .cachedCharges(
                            Arrays.asList(
                                VehicleCharge.builder()
                                    .tariffCode("t-1")
                                    .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                                    .charge(BigDecimal.valueOf(2.56))
                                    .build(),
                                VehicleCharge.builder()
                                    .tariffCode("t-2")
                                    .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                                    .charge(BigDecimal.valueOf(4.89))
                                    .build()
                            )
                        )
                        .vrn("ABC456")
                        .exempt(false)
                        .retrofitted(false)
                        .vehicleType("Van")
                        .build()
                )
                .put(
                    "BX92 CNE",
                    VehicleWithCharges.builder()
                        .cachedCharges(
                            Arrays.asList(
                                VehicleCharge.builder()
                                    .tariffCode("t-10")
                                    .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                                    .charge(BigDecimal.valueOf(9.65))
                                    .build(),
                                VehicleCharge.builder()
                                    .tariffCode("t-20")
                                    .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                                    .charge(BigDecimal.valueOf(12.94))
                                    .build()
                            )
                        )
                        .vrn("BX92 CNE")
                        .exempt(false)
                        .retrofitted(false)
                        .vehicleType("Coach")
                        .build()
                )
                .put(
                    "CAS123",
                    VehicleWithCharges.builder()
                        .cachedCharges(
                            Arrays.asList(
                                VehicleCharge.builder()
                                    .tariffCode(null)
                                    .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                                    .charge(new BigDecimal("0.00"))
                                    .build(),
                                VehicleCharge.builder()
                                    .tariffCode(null)
                                    .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                                    .charge(new BigDecimal("0.00"))
                                    .build()
                            )
                        )
                        .vrn("CAS123")
                        .exempt(true)
                        .retrofitted(true)
                        .vehicleType("HGV")
                        .build()
                )
                .build()
        ),
        Arguments.arguments("1", "2", "3", "CAS123;DKC789",
            ImmutableMap.builder()
                .put(
                    "CAS123",
                    VehicleWithCharges.builder()
                        .cachedCharges(
                            Arrays.asList(
                                VehicleCharge.builder()
                                    .tariffCode(null)
                                    .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                                    .charge(new BigDecimal("0.00"))
                                    .build(),
                                VehicleCharge.builder()
                                    .tariffCode(null)
                                    .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                                    .charge(new BigDecimal("0.00"))
                                    .build()
                            )
                        )
                        .vrn("BX92 CNE")
                        .exempt(true)
                        .retrofitted(true)
                        .vehicleType("HGV")
                        .build()
                )
                .put(
                    "DKC789",
                    VehicleWithCharges.builder()
                        .cachedCharges(
                            Arrays.asList(
                                VehicleCharge.builder()
                                    .tariffCode(null)
                                    .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                                    .charge(null)
                                    .build(),
                                VehicleCharge.builder()
                                    .tariffCode(null)
                                    .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                                    .charge(null)
                                    .build()
                            )
                        )
                        .vrn("BX92 CNE")
                        .exempt(true)
                        .retrofitted(true)
                        .vehicleType("Van")
                        .build()
                )
                .build()
        ),
        Arguments.arguments("5", "1", "6", "PO44 BCN",
            ImmutableMap.of(
                "PO44 BCN",
                VehicleWithCharges.builder()
                    .cachedCharges(Arrays.asList(
                        VehicleCharge.builder()
                            .tariffCode(null)
                            .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                            .charge(null)
                            .build(),
                        VehicleCharge.builder()
                            .tariffCode(null)
                            .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                            .charge(null)
                            .build()
                    ))
                    .vrn("PO44 BCN")
                    .exempt(false)
                    .retrofitted(false)
                    .vehicleType("Van")
                    .build()
            )
        ),
        Arguments.arguments("1", "4", "2", "KQ93NFL;PO44 BCN",
            ImmutableMap.of(
                "PO44 BCN",
                VehicleWithCharges.builder()
                    .cachedCharges(Arrays.asList(
                        VehicleCharge.builder()
                            .tariffCode(null)
                            .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                            .charge(null)
                            .build(),
                        VehicleCharge.builder()
                            .tariffCode(null)
                            .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                            .charge(null)
                            .build()
                    ))
                    .vrn("PO44 BCN")
                    .exempt(false)
                    .retrofitted(false)
                    .vehicleType("Van")
                    .build(),

                "KQ93NFL",
                VehicleWithCharges.builder()
                    .cachedCharges(Arrays.asList(
                        VehicleCharge.builder()
                            .tariffCode(null)
                            .cazId(UUID.fromString("4a09097a-2175-4146-b7df-90dd9e58ac5c"))
                            .charge(null)
                            .build(),
                        VehicleCharge.builder()
                            .tariffCode(null)
                            .cazId(UUID.fromString("4b384b63-0c0e-4979-90bf-aee3b7b51c3a"))
                            .charge(null)
                            .build()
                    ))
                    .vrn("KQ93NFL")
                    .exempt(false)
                    .retrofitted(false)
                    .vehicleType("Bus")
                    .build()
            )
        )
    );
  }
}
