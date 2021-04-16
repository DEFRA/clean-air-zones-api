package uk.gov.caz.whitelist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.whitelist.controller.ExportCsvToS3Controller.BASE_PATH;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.whitelist.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.whitelist.dto.ExportCsvResponseDto;
import uk.gov.caz.whitelist.testutils.AwsS3Helpers;

@MockedMvcIntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-whitelist-vehicles-data.sql",
    "classpath:data/sql/whitelist-vehicles-data.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-whitelist-vehicles-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class ExportCsvToS3ControllerTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";
  private static final String S3_BUCKET = "test-bucket-for-csv-export";
  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");
  public static final String PRESIGNED_URL_REGEX =
      "http://local(host|stack):4572/test-bucket-for-csv-export/"
          + "whitelist_vehicles_\\d{4}-\\d{2}-\\d{2}_\\d{6}"
          + ".csv\\?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=.*";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AmazonS3 amazonS3;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    createS3Bucket();
  }

  @AfterEach
  public void tearDown() {
    deleteS3BucketWithAllContents();
  }

  @Test
  public void shouldReturnDetailsOfAnExportedCsvFile() throws Exception {
    String responseJsonAsString = mockMvc.perform(post(BASE_PATH)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bucketName", is(S3_BUCKET)))
        .andExpect(jsonPath("$.fileUrl", matchesPattern(PRESIGNED_URL_REGEX)))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String actualExportedFileContents = readActualExportedFileContentsFromS3(responseJsonAsString);
    String expectedExportedFileContents = readExpectedExportedFileContents(
        "expected-exported-whitelist-vehicles.csv");

    assertThat(actualExportedFileContents).isEqualTo(expectedExportedFileContents);
  }

  private void createS3Bucket() {
    amazonS3.createBucket(S3_BUCKET);
  }

  private void deleteS3BucketWithAllContents() {
    AwsS3Helpers.deleteBucketWithObjects(S3_BUCKET, amazonS3);
  }

  private String readActualExportedFileContentsFromS3(String responseJsonAsString)
      throws java.io.IOException {
    ExportCsvResponseDto exportCsvResponseDto = objectMapper
        .readValue(responseJsonAsString, ExportCsvResponseDto.class);

    String destinationPath = exportCsvResponseDto.getFileUrl();
    return new RestTemplate().getForObject(destinationPath, String.class);
  }

  private String readExpectedExportedFileContents(String expectedExportedFileName)
      throws IOException {
    Path pathToExpectedResults = FILE_BASE_PATH.resolve(expectedExportedFileName);
    return FileUtils.readFileToString(pathToExpectedResults.toFile());
  }
}