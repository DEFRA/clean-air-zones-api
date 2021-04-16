package uk.gov.caz.taxiregister.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import uk.gov.caz.taxiregister.dto.Email;

@Slf4j
@Service
public class SesEmailSender {

  private final SesClient sesClient;
  private final String senderEmail;

  public SesEmailSender(
      SesClient sesClient,
      @Value("${aws.ses.senderEmail}") String senderEmail) {
    this.sesClient = sesClient;
    this.senderEmail = senderEmail;
  }

  /**
   * Entry point to the class, it sends an email.
   */
  public void send(Email email) {
    log.info("Sending email with title {} and sender email {}", email.getSubject(), senderEmail);

    SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
        .source(senderEmail)
        .destination(Destination.builder().toAddresses(email.getTo()).build())
        .message(Message.builder()
            .subject(Content.builder().charset("UTF-8").data(email.getSubject()).build())
            .body(
                Body.builder()
                    .html(Content.builder()
                        .charset("UTF-8")
                        .data(email.getBody())
                        .build())
                    .build())
            .build())
        .build();

    try {
      sesClient.sendEmail(sendEmailRequest);
    } catch (Exception e) {
      log.error("Cannot send an email", e);
    }
  }
}