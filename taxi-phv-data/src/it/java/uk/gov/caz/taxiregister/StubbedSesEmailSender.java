package uk.gov.caz.taxiregister;

import java.util.LinkedList;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.dto.Email;
import uk.gov.caz.taxiregister.service.SesEmailSender;

public class StubbedSesEmailSender extends SesEmailSender {

  private Queue<Email> emailQueue = new LinkedList<>();

  public StubbedSesEmailSender() {
    super(null, null);
  }

  @Override
  public void send(Email email) {
    emailQueue.add(email);
  }

  public Queue<Email> getEmailQueue() {
    return emailQueue;
  }
}
