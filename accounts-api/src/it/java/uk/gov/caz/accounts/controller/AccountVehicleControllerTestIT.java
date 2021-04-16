package uk.gov.caz.accounts.controller;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import retrofit2.Response;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.AccountVehicleRequest;
import uk.gov.caz.accounts.repository.VccsRepository;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

@MockedMvcIntegrationTest
public class AccountVehicleControllerTestIT {

  public static final UUID CLEAN_AIR_ZONE_1_ID = UUID.randomUUID();
  public static final UUID CLEAN_AIR_ZONE_2_ID = UUID.randomUUID();
  public static final String BUCKET_NAME = "fleet-csv-export-bucket";
  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private S3Client s3Client;

  @MockBean
  private VccsRepository vccsRepository;

  private static final String ACCOUNT_VEHICLE_PATH = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
  private static final String CSV_EXPORT_PATH =
      AccountVehiclesController.ACCOUNT_VEHICLES_PATH + "/csv-exports";
  private static final String ACCOUNT_VEHICLE_DELETE_PATH = ACCOUNT_VEHICLE_PATH + "/{vrn}";

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";

  private static final String EXISTING_ACCOUNT_ID = "457a23f1-3df9-42b9-a42e-435aef201d93";
  private static final String NON_EXISTING_ACCOUNT_ID = "b6968560-cb56-4248-9f8f-d75b0aff726e";

  private static final String EXISTING_VRN = "CAS123";
  private static final String NON_EXISTING_VRN = "CAS246";

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  @BeforeEach
  public void setUpDb() {
    executeSqlFrom("data/sql/delete-account-vehicle-data.sql");
    executeSqlFrom("data/sql/add-account.sql");
    executeSqlFrom("data/sql/add-account-vehicle.sql");
    executeSqlFrom("data/sql/add-account-vehicle-and-chargeability.sql");
    executeSqlFrom("data/sql/create-vehicles-and-chargeability-cache-data-for-csv.sql");
  }

  @AfterEach
  public void cleanUpDb() {
    executeSqlFrom("data/sql/delete-account-vehicle-data.sql");
  }

  @Nested
  class DeleteAccountVehicle {

    @Test
    public void shouldReturn204WhenAccountIdExistsAndVrnExists() throws Exception {
      verifyAccountVehiclesWithVrnCount(EXISTING_VRN, 1);

      performDeleteRequestForAccountWithVrn(EXISTING_ACCOUNT_ID, EXISTING_VRN)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      verifyAccountVehiclesWithVrnCount(EXISTING_VRN, 0);
    }

    @Test
    public void shouldReturn204WhenAccountIdExistsAndVrnDoesNotExist() throws Exception {
      performDeleteRequestForAccountWithVrn(EXISTING_ACCOUNT_ID, NON_EXISTING_VRN)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturn404WhenAccountIdDoesNotExist() throws Exception {
      performDeleteRequestForAccountWithVrn(NON_EXISTING_ACCOUNT_ID, EXISTING_VRN).
          andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn400WhenParamsAreIncorrect() throws Exception {
      String tooLongVrn = "DEFINETLYTOOLONGVRN";

      performDeleteRequestForAccountWithVrn(NON_EXISTING_ACCOUNT_ID, tooLongVrn)
          .andExpect(status().isBadRequest());
    }

    private ResultActions performDeleteRequestForAccountWithVrn(String accountId, String vrn)
        throws Exception {
      return mockMvc.perform(delete(ACCOUNT_VEHICLE_DELETE_PATH, accountId, vrn)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  class CreateAccountVehicle {

    @Test
    public void shouldReturn201WhenAccountIdExistsAndVrnDoesNotExist() throws Exception {
      verifyAccountVehiclesWithVrnCount(NON_EXISTING_VRN, 0);
      mockVccsBulkComplianceCheck();
      mockVccsFetchCazesCall();

      String payload = requestWithVrn(NON_EXISTING_VRN);
      performPostRequestForAccountWithPayload(EXISTING_ACCOUNT_ID, payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.vrn").value(NON_EXISTING_VRN))
          .andExpect(jsonPath("$.accountId").value(EXISTING_ACCOUNT_ID));

      verifyAccountVehiclesWithVrnCount(NON_EXISTING_VRN, 1);
      verifyVehicleChargeabilityWithVrnCount(14);
    }

    @Test
    public void shouldReturn422WhenAccountIdExistsAndVrnExists() throws Exception {
      verifyAccountVehiclesWithVrnCount(EXISTING_VRN, 1);
      String payload = requestWithVrn(EXISTING_VRN);

      performPostRequestForAccountWithPayload(EXISTING_ACCOUNT_ID, payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("AccountVehicle already exists"));

      verifyAccountVehiclesWithVrnCount(EXISTING_VRN, 1);
    }

    @Test
    public void shouldReturn404WhenAccountIdDoesNotExist() throws Exception {
      String payload = requestWithVrn(NON_EXISTING_VRN);

      performPostRequestForAccountWithPayload(NON_EXISTING_ACCOUNT_ID, payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn400WhenParamsAreIncorrect() throws Exception {
      String payload = requestWithVrn("DEFINITELYTOOLONGVRN");

      performPostRequestForAccountWithPayload(NON_EXISTING_ACCOUNT_ID, payload)
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("$.message").value("VRN is too long"));
    }

    private ResultActions performPostRequestForAccountWithPayload(String accountId, String payload)
        throws Exception {
      return mockMvc.perform(post(ACCOUNT_VEHICLE_PATH, accountId)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithVrn(String vrn) {
      AccountVehicleRequest request = AccountVehicleRequest.builder().vrn(vrn).build();
      return toJson(request);
    }

    private void mockVccsFetchCazesCall() {
      when(vccsRepository.findCleanAirZonesSync()).thenReturn(Response.success(
          CleanAirZonesDto.builder().cleanAirZones(
              asList(CleanAirZoneDto.builder().cleanAirZoneId(CLEAN_AIR_ZONE_1_ID).name("caz1")
                      .build(),
                  CleanAirZoneDto.builder().cleanAirZoneId(CLEAN_AIR_ZONE_2_ID).name("caz2").build()
              )
          ).build()));
    }

    private void mockVccsBulkComplianceCheck() {
      ComplianceOutcomeDto caz1ComplianceOutcome = ComplianceOutcomeDto.builder()
          .cleanAirZoneId(CLEAN_AIR_ZONE_1_ID)
          .charge(10.0f)
          .tariffCode("Tariff 1")
          .build();
      ComplianceOutcomeDto caz2ComplianceOutcome = ComplianceOutcomeDto.builder()
          .cleanAirZoneId(CLEAN_AIR_ZONE_2_ID)
          .charge(15.0f)
          .tariffCode("Tariff 1")
          .build();
      List<ComplianceOutcomeDto> complianceOutcomeDtos = newArrayList(caz1ComplianceOutcome,
          caz2ComplianceOutcome);

      ComplianceResultsDto complianceResultsDto = ComplianceResultsDto.builder()
          .isExempt(false)
          .isRetrofitted(false)
          .vehicleType("Car")
          .registrationNumber(NON_EXISTING_VRN)
          .complianceOutcomes(complianceOutcomeDtos)
          .build();
      List<ComplianceResultsDto> complianceResultsDtos = newArrayList(complianceResultsDto);
      when(vccsRepository.findComplianceInBulkSync(newHashSet(NON_EXISTING_VRN)))
          .thenReturn(
              Response.success(complianceResultsDtos));
    }
  }

  @Nested
  class GetAccountVehicle {

    @Test
    public void shouldGetAccountVehicle() throws Exception {
      mockMvc
          .perform(get(AccountsController.ACCOUNTS_PATH + "/{accountId}/vehicles/{vrn}",
              UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c"), "VRN1")
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.vrn").value("VRN1"))
          .andExpect(jsonPath("$.vehicleType").value("Van"))
          .andExpect(jsonPath("$.isExempt").value(false))
          .andExpect(jsonPath("$.isRetrofitted").value(false))
          .andExpect(
              jsonPath("$.cachedCharges[0].cazId").value("39e54ed8-3ed2-441d-be3f-38fc9b70c8d3"))
          .andExpect(jsonPath("$.cachedCharges[0].charge").value(12))
          .andExpect(jsonPath("$.cachedCharges[0].tariffCode").value("Tariff 1"))
          .andExpect(
              jsonPath("$.cachedCharges[1].cazId").value("53e03a28-0627-11ea-9511-ffaaee87e375"))
          .andExpect(jsonPath("$.cachedCharges[1].charge").value(15))
          .andExpect(jsonPath("$.cachedCharges[1].tariffCode").value("Tariff 2"));
    }
  }
  @Nested
  class CsvExport {

    @Test
    public void shouldExportCsvAndReturnResponse() throws Exception {
      createBucketInS3();
      mockVccsFetchCazesCall();

      mockMvc
          .perform(
              post(CSV_EXPORT_PATH, UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a"))
                  .accept(MediaType.APPLICATION_JSON)
                  .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.fileUrl").isNotEmpty())
          .andExpect(jsonPath("$.bucketName").value("fleet-csv-export-bucket"));

      deleteBucketAndFilesFromS3();
    }

    private void mockVccsFetchCazesCall() {

      CleanAirZoneDto birmingham = CleanAirZoneDto.builder()
          .cleanAirZoneId(UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375"))
          .name("Birmingham")
          .activeChargeStartDate("2018-10-28")
          .build();

      CleanAirZoneDto test = CleanAirZoneDto.builder()
          .cleanAirZoneId(UUID.fromString("742b343f-6ce6-42d3-8324-df689ad4c515"))
          .name("Test")
          .activeChargeStartDate("2018-10-29")
          .build();

      CleanAirZoneDto bath = CleanAirZoneDto.builder()
          .cleanAirZoneId(UUID.fromString("59932efc-3190-435c-bddf-2c03385d27f8"))
          .name("Bath")
          .activeChargeStartDate("2040-10-29")
          .build();

      CleanAirZoneDto leicester = CleanAirZoneDto.builder()
          .cleanAirZoneId(UUID.fromString("5e51746b-648d-48ff-8c35-d0aad1dc0fdf"))
          .name("Leicester")
          .activeChargeStartDate("2040-10-29")
          .build();

      when(vccsRepository.findCleanAirZonesSync()).thenReturn(Response.success(
          CleanAirZonesDto.builder().cleanAirZones(
              asList(birmingham, test, bath, leicester)
          ).build()));
    }

    private void createBucketInS3() {
      s3Client
          .createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
    }

    private void deleteBucketAndFilesFromS3() {
      deleteFilesFromS3();
      s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
    }

    private void deleteFilesFromS3() {
      ListObjectsResponse listObjectsResponse = s3Client
          .listObjects(ListObjectsRequest.builder().bucket(BUCKET_NAME).build());
      for (S3Object s3Object : listObjectsResponse.contents()) {
        s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME).key(s3Object.key()));
      }
    }
  }

  @Test
  public void shouldGetValidationErrorAccountVehicles() throws Exception {
    mockMvc
        .perform(get(AccountsController.ACCOUNTS_PATH + "/{accountId}/vehicles", UUID.randomUUID())
            .accept(MediaType.APPLICATION_JSON)
            .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());
  }

  private void verifyAccountVehiclesWithVrnCount(String vrn, int quantity) {
    int accountVehiclesCount = JdbcTestUtils.countRowsInTableWhere(
        jdbcTemplate,
        "caz_account.t_account_vehicle",
        "vrn = \'" + vrn + "\'");

    assertThat(accountVehiclesCount).isEqualTo(quantity);
  }

  private void verifyVehicleChargeabilityWithVrnCount(int quantity) {
    int vehicleChargeability = JdbcTestUtils.countRowsInTable(
        jdbcTemplate, "caz_account.t_vehicle_chargeability");

    assertThat(vehicleChargeability).isEqualTo(quantity);
  }

  @SneakyThrows
  private String toJson(Object object) {
    return objectMapper.writeValueAsString(object);
  }
}
