package uk.gov.caz.testlambda.dto;

import lombok.Value;

@Value
public class VerifyEmailDto {

  String userPoolId;
  String userName;
}
