package uk.gov.caz.testlambda.dto;

import lombok.Value;

@Value
public class NewCognitoUserDto {

  String userPoolId;
  String userName;
  String email;
  String password;
}
