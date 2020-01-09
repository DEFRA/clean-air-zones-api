package uk.gov.caz.testlambda.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.testlambda.dto.LoginUserDto;
import uk.gov.caz.testlambda.dto.NewCognitoUserDto;
import uk.gov.caz.testlambda.dto.VerifyEmailDto;

public interface CognitoControllerApiSpec {

  @PostMapping(CognitoController.PATH)
  ResponseEntity<String> createUser(@RequestBody NewCognitoUserDto newUserData);

  @PostMapping(CognitoController.PATH + "/login")
  ResponseEntity<String> loginUser(@RequestBody LoginUserDto loginUserData);

  @PostMapping(CognitoController.PATH + "/verify-email")
  ResponseEntity<String> verifyEmail(@RequestBody VerifyEmailDto verifyEmailData);

  @GetMapping(CognitoController.PATH + "/user/{username}")
  ResponseEntity<String> getUser(@PathVariable("username") String username,
      @RequestParam("userPoolId") String userPoolId);
}