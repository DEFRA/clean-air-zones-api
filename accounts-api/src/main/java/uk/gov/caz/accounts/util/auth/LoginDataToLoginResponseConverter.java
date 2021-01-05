package uk.gov.caz.accounts.util.auth;

import static java.util.stream.Collectors.toList;

import java.util.List;
import lombok.experimental.UtilityClass;
import uk.gov.caz.accounts.dto.LoginResponseDto;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.LoginData;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * A utility class that converts instances of {@link UserEntity} to {@link LoginResponseDto}.
 */
@UtilityClass
public class LoginDataToLoginResponseConverter {

  /**
   * Converts {@code loginData} to an instance of {@link LoginResponseDto}.
   *
   * @param loginData An instance of {@link LoginData} that needs to be converted.
   * @return An instance of {@link LoginResponseDto} with attributes set from {@code loginData}.
   */
  public static LoginResponseDto from(LoginData loginData) {
    UserEntity user = loginData.getUser();
    Account account = loginData.getAccount();
    List<String> permissions = loginData.getPermissions().stream().map(Enum::toString)
        .collect(toList());
    return LoginResponseDto.builder()
        .accountId(user.getAccountId())
        .accountUserId(user.getId())
        .accountName(account.getName())
        .email(user.getEmail())
        .owner(user.isOwner())
        .betaTester(loginData.isBetaTester())
        .passwordUpdateTimestamp(loginData.getPasswordUpdateTimestamp())
        .permissions(permissions)
        .build();
  }
}
