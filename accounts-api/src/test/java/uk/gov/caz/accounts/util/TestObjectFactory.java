package uk.gov.caz.accounts.util;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import uk.gov.caz.accounts.dto.LoginRequestDto;

public class TestObjectFactory {

  static Stream<Arguments> loginRequestTestDataAndExceptionMessage() {
    return Stream.of(
        Arguments.arguments(buildLogin(null, "valid password"),
            "Email cannot be blank."),
        Arguments.arguments(buildLogin("", "valid password"),
            "Email cannot be blank."),
        Arguments.arguments(buildLogin("             ", "valid password"),
            "Email cannot be blank."),
        Arguments.arguments(buildLogin("test@gov.uk", ""),
            "Password cannot be blank."),
        Arguments.arguments(buildLogin("test@gov.uk", "       "),
            "Password cannot be blank."),
        Arguments.arguments(buildLogin("test@gov.uk", null),
            "Password cannot be blank."),
        Arguments.arguments(
            buildLogin("dsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasds"
                    + "dsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasds"
                    + "dsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasds"
                    + "dsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasds"
                    + "dsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasds",
                "valid password"),
            "Email is too long"),
        Arguments.arguments(buildLogin("test@gov.uk",
            "dsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasds"
                + "daspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjda"
                + "fjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfj"
                + "aofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsa"
                + "jdasdsadfsafjdaspfjdsaofjdasdsadfsafjdaspfjdsaofjdas"),
            "Password is too long")
    );
  }

  private static LoginRequestDto buildLogin(String email, String password) {
    return LoginRequestDto.builder()
        .email(email)
        .password(password)
        .build();
  }
}