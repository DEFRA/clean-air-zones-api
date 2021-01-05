//package uk.gov.caz.accounts.repository;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//
//import java.util.UUID;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import uk.gov.caz.accounts.annotation.IntegrationTest;
//
//@IntegrationTest
//public class StubbedIdentityProviderTestIT {
//
//  @Autowired
//  private IdentityProvider identityProvider;
//
//  @Test
//  public void shouldCreateUser() {
//    //given
//    String email = "test@informed.com";
//    String password = "password";
//
//    //when
//    UUID identityProviderId = identityProvider.createAdminUser(identityProviderId, email, password);
//
//    //then
//    assertThat(identityProvider.getUser(email).getIdentityProviderUserId())
//        .isEqualTo(identityProviderId);
//  }
//
//  @Test
//  public void shouldConfirmThatUserExists() {
//    //given
//    String email = "test@informed.com";
//    String password = "password";
//
//    //when
//    identityProvider.createAdminUser(identityProviderId, email, password);
//
//    //then
//    assertThat(identityProvider.checkIfUserExists(email)).isEqualTo(true);
//  }
//
//  @Test
//  public void shouldLoginUser() {
//    //given
//    String password = "password";
//    String email = "test@informed.com";
//    UUID registeredIdentityProviderId = identityProvider.createAdminUser(identityProviderId, email, password);
//
//    //when
//    UUID identityProviderUserId = identityProvider.loginUser(email, password);
//
//    //then
//    assertThat(identityProviderUserId).isEqualTo(registeredIdentityProviderId);
//  }
//
//  @Test
//  public void shouldNotLoginUserWhenPasswordDoesntMatch() {
//    //given
//    String password = "password";
//    String email = "test@informed.com";
//    identityProvider.createAdminUser(identityProviderId, email, password);
//
//    //when
//    Throwable throwable = catchThrowable(() -> identityProvider.loginUser(email, password + "a"));
//
//    //then
//    assertThat(throwable).isInstanceOf(RuntimeException.class);
//  }
//}
