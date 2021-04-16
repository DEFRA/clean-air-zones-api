package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.dto.Email;
import uk.gov.caz.taxiregister.dto.JobFailureData;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendingValidationErrorsHandler {

  public static final String EMAIL_WITH_ERRORS_TEMPLATE_FILE = "emails/validation-errors.txt";
  private static final String EMAIL_SUBJECT = "Taxi & PHV Database- CSV file error messages";
  private final SesEmailSender sesEmailSender;

  /**
   * Sends an email with validation errors unless the recipient is not specified or the finished
   * job is in invalid state.
   * @param jobFailureData An object containing data which is used to populate the email's body.
   */
  public void handle(JobFailureData jobFailureData) {
    sendEmailWithErrors(jobFailureData);
  }

  private void sendEmailWithErrors(JobFailureData jobFailureData) {
    if (RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS != jobFailureData.getJobStatus()) {
      log.info("Not sending an email as status doesn't indicate failure because of validation "
          + "errors");
      return;
    }

    String recipient = jobFailureData.getOwnerEmail().orElse(null);
    if (recipient == null) {
      log.warn("Not sending an email because owner email is empty");
      return;
    }

    sesEmailSender.send(prepareEmail(recipient, jobFailureData.getValidationErrors()));
  }

  private Email prepareEmail(String recipient, List<ValidationError> validationErrors) {
    return Email.builder()
        .body(prepareBody(validationErrors, EMAIL_WITH_ERRORS_TEMPLATE_FILE))
        .subject(EMAIL_SUBJECT)
        .to(recipient)
        .build();
  }

  @VisibleForTesting
  @SneakyThrows
  String prepareBody(List<ValidationError> validationErrors, String template) {
    String errorLines = validationErrors
        .stream()
        .map(ValidationError::getDetail)
        .collect(Collectors.joining("<br>"));

    String emailTemplate = Resources.toString(
        Resources.getResource(template),
        StandardCharsets.UTF_8
    );

    return emailTemplate.replaceFirst("!errors-placeholder!", errorLines);
  }
}