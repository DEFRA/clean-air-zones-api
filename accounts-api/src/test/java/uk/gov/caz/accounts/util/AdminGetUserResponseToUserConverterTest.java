package uk.gov.caz.accounts.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;

class AdminGetUserResponseToUserConverterTest {

  private String VALID_EMAIL = "jan@kowalski.com";
  private String VALID_NAME = "any-janek";
  private String VALID_IDENTITY_PROVIDER_ID = "c7acc022-0c15-477e-a576-3c14c889fda9";

  @Test
  void shouldReturnUserObjectWhenEmailIsFoundInAttributesList() {
    // given
    AdminGetUserResponse response = buildGetAdminUserResponse();

    // when
    User user = AdminGetUserResponseToUserConverter.convert(response);

    // then
    assertThat(user.getEmail()).isEqualTo(VALID_EMAIL);
    assertThat(user.getName()).isEqualTo(VALID_NAME);
    assertThat(user.isEmailVerified()).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenProvidedEmailIsNotFoundInAttributesList() {
    // given
    AdminGetUserResponse response = AdminGetUserResponse.builder()
        .userAttributes(AttributeType.builder().name("name").value(VALID_NAME).build())
        .build();

    //when
    Throwable throwable = catchThrowable(
        () -> AdminGetUserResponseToUserConverter.convert(response));

    //then
    assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
    assertThat(throwable).hasMessage("External Service Failure");
  }

  @Nested
  class ToUserEntity {

    @Test
    void shouldConvertEmptyStringOfLockoutTimeToNull() {
      // given
      AdminGetUserResponse adminGetUserResponse = buildGetAdminUserResponse();
      AdminGetUserResponse response = adminGetUserResponse.toBuilder()
          .userAttributes(
              ImmutableList.<AttributeType>builder()
                  .addAll(adminGetUserResponse.userAttributes())
                  .add(AttributeType.builder().name("custom:lockout-time").value("").build())
                  .build())
          .build();

      //when
      UserEntity userEntity = AdminGetUserResponseToUserConverter.convertToUserEntity(response);

      //then
      assertThat(userEntity.getLockoutTime()).isEmpty();
    }

  }

  private AdminGetUserResponse buildGetAdminUserResponse() {
    AttributeType emailAttribute = AttributeType.builder().name("email").value(VALID_EMAIL)
        .build();
    AttributeType subAttribute = AttributeType.builder().name("preferred_username").value(
        VALID_IDENTITY_PROVIDER_ID)
        .build();
    AttributeType emailVerifiedAttribute = AttributeType.builder()
        .name("email_verified")
        .value("true")
        .build();
    AttributeType nameAttribute = AttributeType.builder().name("name").value(VALID_NAME).build();

    return AdminGetUserResponse.builder()
        .userAttributes(Arrays.asList(emailAttribute, subAttribute, emailVerifiedAttribute,
            nameAttribute))
        .userCreateDate(Instant.parse("2020-01-10T10:15:30.00Z"))
        .build();
  }

}