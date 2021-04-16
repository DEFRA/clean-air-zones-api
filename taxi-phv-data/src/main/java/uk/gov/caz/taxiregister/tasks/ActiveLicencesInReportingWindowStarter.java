package uk.gov.caz.taxiregister.tasks;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow;
import uk.gov.caz.taxiregister.model.LicenceEvent;
import uk.gov.caz.taxiregister.service.ReportingService;

/**
 * This class act as a task runner entry point for running reporting query for active licences in
 * specified reporting window. For more details about tasks check TASKS_HOWTO.md readme file in the
 * same package.
 */
@Component
@ConditionalOnProperty(
    value = "tasks.active-licences-in-reporting-window.enabled",
    havingValue = "true")
@Slf4j
@AllArgsConstructor
public class ActiveLicencesInReportingWindowStarter implements ApplicationRunner {

  private static final int REQUIRED_NUMBER_OF_ARGUMENTS = 3;
  private ReportingService reportingService;
  private ActiveLicencesInReportingWindowOutput output;

  @Override
  public void run(ApplicationArguments args) {
    if (inTestInitPhase(args)) {
      return;
    }

    log.info("Executing task: ActiveLicencesInReportingWindowStarter");

    if (args.getNonOptionArgs().size() != REQUIRED_NUMBER_OF_ARGUMENTS) {
      System.exit(logErrorAboutInvalidParametersAndProperUsage());
    }

    try {
      LocalDate startDate = LocalDate.parse(args.getNonOptionArgs().get(0));
      LocalDate endDate = LocalDate.parse(args.getNonOptionArgs().get(1));
      String csvFile = args.getNonOptionArgs().get(2);
      if (startDate.isEqual(endDate) || startDate.isAfter(endDate) || csvFile.isEmpty()) {
        System.exit(logErrorAboutInvalidParametersAndProperUsage());
      }
      log.info("Reporting window: <{} - {}>", startDate.toString(), endDate.toString());
      output.writeToCsv(reportingService.runReporting(startDate, endDate), csvFile);
    } catch (DateTimeParseException parseException) {
      System.exit(logErrorAboutInvalidParametersAndProperUsage());
    } finally {
      log.info("Task finished: ActiveLicencesInReportingWindowStarter");
    }
  }

  /**
   * Checks whether this bean is executing as Spring startup code during initialization of test
   * phase.
   */
  private boolean inTestInitPhase(ApplicationArguments args) {
    if (args.getNonOptionArgs().size() == 1 && args.getNonOptionArgs().get(0)
        .equalsIgnoreCase("in-integration-test-initialization")) {
      log.info(
          "Skipping due to test initialization phase: "
              + "Executing task: ActiveLicencesInReportingWindowStarter");
      return true;
    }
    return false;
  }

  /**
   * Logs error if parameters are invalid and provides return error code.
   */
  private int logErrorAboutInvalidParametersAndProperUsage() {
    log.error("Task requires 3 parameters: start date, end date and csv file path/name"
        + "An example 'java -jar task.jar 2020-05-01 2020-07-01 report.csv'");
    return 1;
  }

  /**
   * Handler for java objects with results of reporting query for active licences in reporting
   * window.
   */
  @Component
  @AllArgsConstructor
  @Getter
  public static class ActiveLicencesInReportingWindowOutput {

    /**
     * Write list of {@link ActiveLicenceInReportingWindow} objects to CSV file.
     *
     * @param activeLicences list of Java objects with results of active licences in reporting
     *     window.
     * @param csvFileName Path and name of output CSV file.
     */
    public void writeToCsv(List<ActiveLicenceInReportingWindow> activeLicences,
        String csvFileName) {
      try {
        CSVWriter writer = new CSVWriter(new FileWriter(csvFileName));
        writer.writeNext(header());
        activeLicences.stream()
            .map(this::arrayOfStrings)
            .forEach(writer::writeNext);
        writer.close();
      } catch (IOException e) {
        log.error("IO Error during writing to CSV file", e);
      }
    }

    /**
     * Forms CSV header.
     */
    private String[] header() {
      return new String[]{
          "STATUS",
          "EVENT TSTMP",
          "VRM",
          "INSERT TSTMP",
          "LICENCE START DATE",
          "LICENCE END DATE",
          "LICENCE PLATE NUMBER",
          "UPLOADER ID",
          "LICENSING AUTHORITY ID",
          "DESCRIPTION",
          "WHEELCHAIR ACCESSIBLE"};
    }

    /**
     * Converts {@link ActiveLicenceInReportingWindow} instance to array of Strings which can be
     * easily written to CSV file.
     */
    private String[] arrayOfStrings(ActiveLicenceInReportingWindow activeLicenceInReportingWindow) {
      LicenceEvent le = activeLicenceInReportingWindow.getLicenceEvent();
      return new String[]{
          activeLicenceInReportingWindow.getStatusInReportingWindow().name(),
          le.getEventTimestamp().toString(),
          le.getVrm(),
          le.getInsertTimestamp().toString(),
          le.getLicenceStartDate().toString(),
          le.getLicenceEndDate().toString(),
          le.getLicencePlateNumber(),
          le.getUploaderId().toString(),
          String.valueOf(le.getLicensingAuthorityId()),
          le.getDescription(),
          String.valueOf(le.getWheelchairAccessible())};
    }
  }
}
