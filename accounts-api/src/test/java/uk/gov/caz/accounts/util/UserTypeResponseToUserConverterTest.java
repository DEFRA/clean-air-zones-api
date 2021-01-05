package uk.gov.caz.accounts.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;

class UserTypeResponseToUserConverterTest {

  private String ANY_EMAIL = "jan@kowalski.com";
  private String ANY_NAME = "any-janek";

  @Test
  void shouldReturnUserObjectWhenEmailIsFoundInAttributesList() {
    // given
    UserType response = buildUserTypeResponse();
    User user = buildRequestedUser();

    // when
    User result = UserTypeResponseToUserConverter.convert(user, response);

    // then
    assertThat(result.getEmail()).isEqualTo(ANY_EMAIL);
    assertThat(result.getName()).isEqualTo(ANY_NAME);
    assertThat(result.getId()).isEqualTo(user.getId());
    assertThat(result.getIdentityProviderUserId()).isEqualTo(user.getIdentityProviderUserId());
  }

  @Test
  void shouldReturnNullForEmailWhenNotFoundInAttributes() {
    // given
    UserType response = UserType.builder()
        .attributes(AttributeType.builder().name("name").value(ANY_NAME).build())
        .build();
    User user = buildRequestedUser();

    //when
    User result = UserTypeResponseToUserConverter.convert(user, response);

    //then
    assertThat(result.getName()).isEqualTo(ANY_NAME);
    assertThat(result.getEmail()).isEqualTo(null);
  }

  @Test
  void shouldReturnNullForNameWhenNotFoundInAttributes() {
    // given
    UserType response = UserType.builder()
        .attributes(AttributeType.builder().name("email").value(ANY_EMAIL).build())
        .build();
    User user = buildRequestedUser();

    //when
    User result = UserTypeResponseToUserConverter.convert(user, response);

    //then
    assertThat(result.getName()).isEqualTo(null);
    assertThat(result.getEmail()).isEqualTo(ANY_EMAIL);
  }

  private UserType buildUserTypeResponse() {
    return UserType.builder()
        .attributes(
            AttributeType.builder().name("email").value(ANY_EMAIL).build(),
            AttributeType.builder().name("name").value(ANY_NAME).build()
        )
        .build();
  }

  private User buildRequestedUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .build();
  }
}