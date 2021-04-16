package uk.gov.caz.accounts;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.util.Sha2Hasher;

public class StubbedIdentityProvider extends IdentityProvider {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  private final Map<String, StubbedIdentityProviderUser> users = new HashMap<>();
  private final Map<String, String> passwords = new HashMap<>();

  public StubbedIdentityProvider() {
    super("", "", "", null);
  }

  @Override
  public boolean checkIfUserExists(String email) {
    return users.containsKey(email);
  }

  @Override
  public User getUser(String email) {
    StubbedIdentityProviderUser stubbedIdentityProviderUser = users.get(email);

    if (stubbedIdentityProviderUser == null) {
      throw new RuntimeException("User doesn't exist");
    }

    return User.builder()
        .email(stubbedIdentityProviderUser.getEmail())
        .identityProviderUserId(stubbedIdentityProviderUser.getIdentityProviderUserId())
        .emailVerified(stubbedIdentityProviderUser.isEmailVerified())
        .name(stubbedIdentityProviderUser.getName())
        .build();
  }

  @Override
  public UserEntity getUserAsUserEntity(String email) {
    StubbedIdentityProviderUser stubbedIdentityProviderUser = users.get(email);

    LocalDateTime lockoutTime = stubbedIdentityProviderUser.getLockoutTime();
    return UserEntity.builder()
        .email(email)
        .name(stubbedIdentityProviderUser.getName())
        .identityProviderUserId(stubbedIdentityProviderUser.getIdentityProviderUserId())
        .isOwner(stubbedIdentityProviderUser.isOwner())
        .emailVerified(stubbedIdentityProviderUser.isEmailVerified())
        .accountId(stubbedIdentityProviderUser.getAccountId())
        .failedLogins(stubbedIdentityProviderUser.getFailedLogins())
        .lockoutTime(lockoutTime)
        .passwordUpdateTimestamp(LocalDateTime.of(2020, 01, 10, 5, 28, 48))
        .build();
  }

  @Override
  public void createAdminUser(UUID identityProviderId, String email, String password) {
    StubbedIdentityProviderUser stubbedIdentityProviderUser = StubbedIdentityProviderUser.builder()
        .email(email)
        .password(password)
        .emailVerified(false)
        .identityProviderUserId(identityProviderId)
        .build();
    users.put(email, stubbedIdentityProviderUser);
    updateLastPasswords(email, password);
  }

  private void updateLastPasswords(String email, String password) {
    List<String> currentPasswords = Arrays.stream(passwords.getOrDefault(email, "").split(","))
        .collect(
            Collectors.toList());
    currentPasswords.add(Sha2Hasher.sha256Hash(password));
    passwords.put(email, String.join(",", currentPasswords));
  }

  @Override
  public UserEntity createStandardUser(UserEntity user) {
    StubbedIdentityProviderUser stubbedIdentityProviderUser = StubbedIdentityProviderUser.builder()
        .accountId(user.getAccountId())
        .name(user.getName())
        .isOwner(false)
        .email(user.getEmail())
        .password(null)
        .emailVerified(user.isEmailVerified())
        .identityProviderUserId(
            user.getIdentityProviderUserId() == null
                ? UUID.randomUUID()
                : user.getIdentityProviderUserId()
        )
        .build();
    users.put(user.getEmail(), stubbedIdentityProviderUser);
    return user.toBuilder()
        .identityProviderUserId(stubbedIdentityProviderUser.getIdentityProviderUserId())
        .build();
  }

  @Override
  public String getEmailByIdentityProviderId(UUID identityProviderId) {
    return users.entrySet().stream()
        .filter(e -> Objects.equals(e.getValue().getIdentityProviderUserId(), identityProviderId))
        .map(Entry::getKey)
        .findFirst()
        .get();
  }

  @Override
  public UserEntity getUserDetailsByIdentityProviderId(UserEntity user) {
    StubbedIdentityProviderUser foundUser = users.entrySet().stream()
        .filter(e -> Objects
            .equals(e.getValue().getIdentityProviderUserId(), user.getIdentityProviderUserId()))
        .map(Entry::getValue)
        .findFirst()
        .orElseThrow(IdentityProviderUnavailableException::new);
    return user.toBuilder()
        .email(foundUser.getEmail())
        .name(foundUser.getName())
        .build();
  }

  @Override
  public void verifyEmail(User user) {
    StubbedIdentityProviderUser oldIdentityProviderUser = users.remove(user.getEmail());
    users.put(user.getEmail(), oldIdentityProviderUser.toBuilder().emailVerified(true).build());
  }

  @Override
  public void verifyEmail(UserEntity user) {
    StubbedIdentityProviderUser oldIdentityProviderUser = users.remove(user.getEmail());
    users.put(user.getEmail(), oldIdentityProviderUser.toBuilder().emailVerified(true).build());
  }

  @Override
  public void setUserPassword(String email, String password) {
    StubbedIdentityProviderUser oldIdentityProviderUser = users.remove(email);
    users.put(email, oldIdentityProviderUser.toBuilder().password(password).build());
    updateLastPasswords(email, password);
  }

  @Override
  public void setUserName(String email, String name) {
    StubbedIdentityProviderUser oldIdentityProviderUser = users.remove(email);
    users.put(email, oldIdentityProviderUser.toBuilder().name(name).build());
  }

  @Override
  public UUID loginUser(String email, String password) {
    return users.values()
        .stream()
        .filter(user -> Objects.equals(user.getEmail(), email))
        .filter(user -> Objects.equals(user.getPassword(), password))
        .findFirst()
        .map(StubbedIdentityProviderUser::getIdentityProviderUserId)
        .orElseThrow(this::getStubbedNotAuthorizedException);
  }

  @Override
  public void deleteUser(String email) {
    users.remove(email);
  }

  @Override
  public void increaseFailedLoginsByOne(String email) {
    StubbedIdentityProviderUser identityProviderUser = users.get(email);
    users.put(email, identityProviderUser
        .toBuilder()
        .failedLogins(getCurrentFailedLogins(email) + 1)
        .build());
  }

  @Override
  public void setLockoutTime(String email) {
    StubbedIdentityProviderUser identityProviderUser = users.get(email);
    users.put(email, identityProviderUser
        .toBuilder()
        .lockoutTime(LocalDateTime.now())
        .build());
  }

  @Override
  public void resetFailedLoginsAndLockoutTime(String email) {
    StubbedIdentityProviderUser identityProviderUser = users.get(email);
    users.put(email, identityProviderUser
        .toBuilder()
        .failedLogins(0)
        .lockoutTime(null)
        .build());
  }

  @Override
  public int getCurrentFailedLogins(String email) {
    UserEntity user = getUserAsUserEntity(email);
    return user.getFailedLogins();
  }

  @Override
  public Optional<LocalDateTime> getCurrentLockoutTime(String email) {
    UserEntity user = getUserAsUserEntity(email);
    return user.getLockoutTime();
  }

  @Override
  public void cloneUserAndSetEmailTo(UUID identityProviderIdOfUserToBeCloned,
      UUID newIdentityProviderUserId, String newEmail) {
    String email = getEmailByIdentityProviderId(identityProviderIdOfUserToBeCloned);
    UserEntity user = getUserAsUserEntity(email);

    StubbedIdentityProviderUser stubbedIdentityProviderUser = StubbedIdentityProviderUser.builder()
        .accountId(user.getAccountId())
        .name(user.getName())
        .isOwner(user.isOwner())
        .email(newEmail)
        .password(null)
        .emailVerified(false)
        .identityProviderUserId(newIdentityProviderUserId)
        .build();
    users.put(newEmail, stubbedIdentityProviderUser);
  }

  @Override
  public List<String> previousPasswordsForUser(String email) {
    if (!passwords.containsKey(email)) {
      return Lists.emptyList();
    }
    return Arrays.asList(passwords.get(email).split(","));
  }

  @Override
  public void clearPreviousPasswordsForUser(String email) {
    passwords.put(email, "");
  }

  public void mockFailedLoginsAndLockoutTime(String email, int failedLogins,
      LocalDateTime lockoutTime) {
    StubbedIdentityProviderUser identityProviderUser = users.get(email);
    users.put(email, identityProviderUser
        .toBuilder()
        .failedLogins(failedLogins)
        .lockoutTime(lockoutTime)
        .build());
  }

  @Override
  public boolean isUserBetaTester(String email) {
    return true;
  }

  private AwsErrorDetails getStubbedAwsNotAuthorizedErrorDetails() {
    return AwsErrorDetails
        .builder()
        .errorCode("NotAuthorizedException")
        .errorMessage("User does not exist.")
        .build();
  }

  private NotAuthorizedException getStubbedNotAuthorizedException() {
    AwsErrorDetails details = getStubbedAwsNotAuthorizedErrorDetails();

    return NotAuthorizedException.builder().awsErrorDetails(details).build();
  }

  public void clearUserData() {
    this.users.clear();
  }
}