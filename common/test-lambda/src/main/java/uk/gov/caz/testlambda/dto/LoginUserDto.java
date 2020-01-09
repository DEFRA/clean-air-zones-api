package uk.gov.caz.testlambda.dto;

import lombok.Value;

@Value
public class LoginUserDto {

  String userPoolId;
  String clientId;
  String clientSecret;
  String userName;
  String password;
}
