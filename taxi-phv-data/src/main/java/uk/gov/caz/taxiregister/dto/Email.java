package uk.gov.caz.taxiregister.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Email {

  private String to;
  private String body;
  private String subject;
}
