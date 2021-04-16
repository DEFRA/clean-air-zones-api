package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.caz.taxiregister.service.EmailSendingValidationErrorsHandler.EMAIL_WITH_ERRORS_TEMPLATE_FILE;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.dto.Email;
import uk.gov.caz.taxiregister.dto.JobFailureData;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.testutils.TestObjects;

@ExtendWith(MockitoExtension.class)
class EmailSendingValidationErrorsHandlerUnitTest {

  public static final ArrayList<ValidationError> VALIDATION_ERRORS = Lists.newArrayList(
      ValidationError.missingFieldError("VRN1", "detail1"),
      ValidationError.missingFieldError("VRN2", "detail2")
  );
  @Mock
  private SesEmailSender sesEmailSender;

  @InjectMocks
  private EmailSendingValidationErrorsHandler validationErrorsHandler;

  @Test
  public void shouldDoNothingWhenJobWasNotFinishedWithValidationErrors() {
    //given
    JobFailureData jobFailureData = JobFailureData.builder()
        .jobStatus(RegisterJobStatus.ABORTED)
        .build();

    //when
    validationErrorsHandler.handle(jobFailureData);

    //then
    verifyNoInteractions(sesEmailSender);
  }

  @Test
  public void shouldDoNothingIfEmailIsEmpty() {
    //given
    JobFailureData jobFailureData = JobFailureData.builder()
        .jobStatus(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS)
        .ownerEmail(Optional.empty())
        .build();

    //when
    validationErrorsHandler.handle(jobFailureData);

    //then
    verifyNoInteractions(sesEmailSender);
  }

  @Test
  public void shouldSendAnEmailForValidJobFailureData() {
    //given
    JobFailureData jobFailureData = JobFailureData.builder()
        .jobStatus(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS)
        .ownerEmail(Optional.of(TestObjects.TYPICAL_UPLOADER_EMAIL))
        .validationErrors(VALIDATION_ERRORS)
        .build();

    //when
    validationErrorsHandler.handle(jobFailureData);

    //then
    verify(sesEmailSender).send(Email.builder()
        .body(validationErrorsHandler.prepareBody(VALIDATION_ERRORS, EMAIL_WITH_ERRORS_TEMPLATE_FILE))
        .to(TestObjects.TYPICAL_UPLOADER_EMAIL)
        .subject("Taxi & PHV Database- CSV file error messages")
        .build());
  }

  @Test
  public void shouldPrepareEmailBodyWithErrors() {
    //when
    String body = validationErrorsHandler.prepareBody(VALIDATION_ERRORS, EMAIL_WITH_ERRORS_TEMPLATE_FILE);

    //then
    assertThat(body).isEqualTo("some text detail1<br>detail2 some text");
  }

  @Test
  public void shouldThrowRuntimeExceptionIfTemplateDoesntExists() {
    assertThrows(RuntimeException.class, () -> {
      validationErrorsHandler.prepareBody(VALIDATION_ERRORS, UUID.randomUUID().toString());
    });
  }

  @Test
  public void shouldNotContainsAnyBrTags() {
    //when
    String body = validationErrorsHandler.prepareBody(VALIDATION_ERRORS, EMAIL_WITH_ERRORS_TEMPLATE_FILE);

    //then
    assertThat(body).isEqualTo("some text detail1<br>detail2 some text");
  }
}