package uk.gov.caz.taxiregister.reporting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._10_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._15_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._1_DAY_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._20_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._2_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._3_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._4_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._6_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._7_DAYS_AGO;
import static uk.gov.caz.taxiregister.reporting.LicensingAuthoritiesThatForgotToUploadReportTestIT.Updated._8_DAYS_AGO;

import com.google.common.collect.ImmutableMap;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/delete-reporting-data.sql",
    "classpath:data/sql/add-reporting-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-reporting-data.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class LicensingAuthoritiesThatForgotToUploadReportTestIT {

  /**
   * Name of Postgres function that provides reporting answer.
   */
  private static final String REPORT_DB_FUNCTION_NAME = "authorities_that_have_not_uploaded_licences_in_last_days";
  /**
   * Name of parameter to Postgres function that provides reporting answer.
   */
  private static final String INPUT_PARAM_NAME = "number_of_days";
  /**
   * Reporting function returns DB Table with 1 column. This is name of this column.
   */
  private static final String OUTPUT_TABLE_COLUMN = "licensing_authority_name";

  @Test
  public void testReportToGetLicensingAuthoritiesThatForgotToUploadLicenceData() {
    thereAreLicensingAuthoritiesNamed("Birmingham", "Leeds", "Liverpool", "London");

    and("Leeds").uploadedData(_3_DAYS_AGO);
    and("Birmingham", "Liverpool").uploadedData(_7_DAYS_AGO);
    and("Leeds").uploadedData(_8_DAYS_AGO);
    and("London").uploadedData(_10_DAYS_AGO);
    and("Leeds", "Birmingham", "Liverpool", "London").uploadedData(_15_DAYS_AGO);

    ifAskedWhoForgotToUpload(_1_DAY_AGO).thereMustBe("Leeds", "Birmingham", "Liverpool", "London");
    ifAskedWhoForgotToUpload(_2_DAYS_AGO).thereMustBe("Leeds", "Birmingham", "Liverpool", "London");
    ifAskedWhoForgotToUpload(_3_DAYS_AGO).thereMustBe("Birmingham", "Liverpool", "London");
    ifAskedWhoForgotToUpload(_4_DAYS_AGO).thereMustBe("Birmingham", "Liverpool", "London");
    ifAskedWhoForgotToUpload(_6_DAYS_AGO).thereMustBe("Birmingham", "Liverpool", "London");
    ifAskedWhoForgotToUpload(_7_DAYS_AGO).thereMustBe("London");
    ifAskedWhoForgotToUpload(_8_DAYS_AGO).thereMustBe("London");
    ifAskedWhoForgotToUpload(_10_DAYS_AGO).thereMustBeNoOne();
    ifAskedWhoForgotToUpload(_20_DAYS_AGO).thereMustBeNoOne();
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final LocalDateTime NOW = LocalDateTime.now();

  public enum Updated {
    _1_DAY_AGO(1),
    _2_DAYS_AGO(2),
    _3_DAYS_AGO(3),
    _4_DAYS_AGO(4),
    _6_DAYS_AGO(6),
    _7_DAYS_AGO(7),
    _8_DAYS_AGO(8),
    _10_DAYS_AGO(10),
    _15_DAYS_AGO(15),
    _20_DAYS_AGO(20);

    private int daysAgo;

    Updated(int daysAgo) {
      this.daysAgo = daysAgo;
    }

    public LocalDateTime getLocalDateTime() {
      switch (this) {
        case _7_DAYS_AGO:
          // For updates done 7 days ago (Birmingham and Liverpool) date of last update
          // is set to minus 7 days from now but on 1 minute past midnight.
          // Why?
          // This is to test a requirement that says that reporting function should not
          // take hours+minutes+seconds but only whole days. For Birmingham and Liverpool
          // we set date to be minus 7 days minus some amount of hours and minutes from now.
          return NOW.minusDays(daysAgo).withHour(0).withMinute(1);
        case _10_DAYS_AGO:
          // For updates done 10 days ago (London) date of last update
          // is set to minus 10 days from now but on hour:minute = 23:59.
          // Why?
          // This is to test a requirement that says that reporting function should not
          // take hours+minutes+seconds but only whole days. For London
          // we set date to be minus 10 days plus some amount of hours and minutes from now.
          return NOW.minusDays(daysAgo).withHour(23).withMinute(59);
        default:
          // For all other updates we take exactly N number of days including hours,
          // minutes and seconds from now.
          return NOW.minusDays(daysAgo);
      }
    }
  }

  private void thereAreLicensingAuthoritiesNamed(String birmingham, String leeds, String liverpool,
      String london) {
    // All required data is loaded by main Liquibase scripts and SQL script 'add-reporting-data' before test method.
    // 4 Licensing Authorities are created:
    // Birmingham with ID = 1
    // Leeds with ID = 2
    // Liverpool with ID = 3
    // London with ID = 4
    //
    // Some not important entry in Register Jobs with ID = 1 required as foreign key
    // for Register Jobs Info entries.
  }

  private LicensingAuthorityDataUploadedAt and(String... licensingAuthorityNames) {
    return new LicensingAuthorityDataUploadedAt(asList(licensingAuthorityNames),
        jdbcTemplate);
  }

  private ReportRunAssertion ifAskedWhoForgotToUpload(Updated updated) {
    return new ReportRunAssertion(jdbcTemplate, updated);
  }

  /**
   * Helper class for putting entries in Register Jobs Info table.
   */
  @AllArgsConstructor
  private static class LicensingAuthorityDataUploadedAt {

    private List<String> licensingAuthorities;
    private JdbcTemplate jdbcTemplate;

    public void uploadedData(Updated updated) {
      String insertSql =
          "INSERT INTO T_MD_REGISTER_JOBS_INFO "
              + "(REGISTER_JOB_ID, LICENCE_AUTHORITY_ID, INSERT_TIMESTMP) "
              + "VALUES (1, ?, ?)";

      jdbcTemplate.update(connection -> {
        PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
        preparedStatement.setArray(1,
            connection.createArrayOf("integer", laNamesToIds(licensingAuthorities).toArray()));
        preparedStatement.setObject(2, updated.getLocalDateTime());
        return preparedStatement;
      });
    }
  }


  /**
   * Helper class to run reporting function and verify results.
   */
  private static class ReportRunAssertion extends StoredProcedure {

    private Updated updated;

    public ReportRunAssertion(JdbcTemplate jdbcTemplate, Updated updated) {
      super(jdbcTemplate, REPORT_DB_FUNCTION_NAME);
      this.updated = updated;
      setFunction(true);
    }

    public void thereMustBe(String... expectedLANames) {
      Set<String> reportedLANames = executeDbFunctionAndGetReportedLicensingAuthorityNamesThatForgotToUpload();
      assertThat(reportedLANames).hasSameSizeAs(expectedLANames);
      assertThat(reportedLANames).containsExactlyInAnyOrder(expectedLANames);
    }

    public void thereMustBeNoOne() {
      Set<String> reportedLANames = executeDbFunctionAndGetReportedLicensingAuthorityNamesThatForgotToUpload();
      assertThat(reportedLANames).isEmpty();
    }

    private Set<String> executeDbFunctionAndGetReportedLicensingAuthorityNamesThatForgotToUpload() {
      SqlParameter numberOfDaysParam = new SqlParameter(INPUT_PARAM_NAME, Types.INTEGER);
      SqlParameter[] paramArray = {numberOfDaysParam};

      setParameters(paramArray);
      compile();

      Map storedProcResult = execute(Integer.valueOf(updated.daysAgo));
      List<Map> reportedLANames = (List<Map>) storedProcResult.get("#result-set-1");

      return reportedLANames.stream()
          .map(ReportRunAssertion::extractLAName)
          .collect(Collectors.toSet());
    }

    private static String extractLAName(Map map) {
      return (String) map.get(OUTPUT_TABLE_COLUMN);
    }
  }

  /**
   * Maps Licensing Authority name to their ID.
   */
  private static Map<String, Integer> laNameToId = ImmutableMap.of(
      "Birmingham", 1,
      "Leeds", 2,
      "Liverpool", 3,
      "London", 4
  );

  private static List<Integer> laNamesToIds(List<String> laNames) {
    return laNames.stream().map(laName -> laNameToId.get(laName)).collect(Collectors.toList());
  }
}
