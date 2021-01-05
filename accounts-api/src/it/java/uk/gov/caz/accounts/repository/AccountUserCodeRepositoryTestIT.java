package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;

@Sql(scripts = {"classpath:data/sql/delete-user-data.sql",
    "classpath:data/sql/create-account-user.sql",
    "classpath:data/sql/create-account-user-code.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
public class AccountUserCodeRepositoryTestIT {

  @Autowired
  private AccountUserCodeRepository accountUserCodeRepository;

  @Test
  void shouldCreateAndFindAccountUserCode() {
    //given
    AccountUserCode accountUserCode = buildAccountUserCode();

    //when
    AccountUserCode createdAccountUserCode = accountUserCodeRepository.save(accountUserCode);
    Optional<AccountUserCode> foundAccountUserCode = accountUserCodeRepository
        .findById(createdAccountUserCode.getId());

    //then
    assertThat(createdAccountUserCode.getId()).isNotNull();
    assertThat(foundAccountUserCode).isPresent();
    assertThat(foundAccountUserCode.get()).isEqualTo(createdAccountUserCode);
  }

  @Test
  @Transactional
  void shouldUpdateStatusOfCode() {
    //given
    String code = "exampleCode";

    //when
    accountUserCodeRepository.setStatusForCode(code, CodeStatus.USED);

    Optional<AccountUserCode> accountUserCode = accountUserCodeRepository.findByCodeAndCodeType(code, CodeType.PASSWORD_RESET);

    //then
    assertThat(accountUserCode).isPresent();
    assertThat(accountUserCode.get().getStatus()).isEqualTo(CodeStatus.USED);
  }

  @Test
  void shouldFindCodeByUserAndStatus() {
    //given
    UUID accountUserId = UUID.fromString("4e581c88-3ba3-4df0-91a3-ad46fb48bfd1");

    //when
    List<AccountUserCode> foundAccountUserCodes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeTypeIn(accountUserId, CodeStatus.ACTIVE,
            Collections.singletonList(CodeType.PASSWORD_RESET));
    List<AccountUserCode> notFoundAccountUserCodes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeTypeIn(accountUserId, CodeStatus.USED,
            Collections.singletonList(CodeType.PASSWORD_RESET));

    //then
    assertThat(foundAccountUserCodes).isNotEmpty();
    assertThat(notFoundAccountUserCodes).isEmpty();
  }

  @Test
  void shouldFindUserCodesByAccountIdAndStatus() {
    List<AccountUserCode> foundAccountUserCodes = accountUserCodeRepository.
        findAllByAccountNameForExistingUsers("test");
    assertThat(foundAccountUserCodes).hasSize(2);
    assertThat(foundAccountUserCodes.get(0).getExpiration())
        .isEqualTo(LocalDateTime.parse("2020-01-01T10:10"));
  }

  private AccountUserCode buildAccountUserCode() {
    return AccountUserCode.builder()
        .accountUserId(UUID.fromString("4e581c88-3ba3-4df0-91a3-ad46fb48bfd1"))
        .code("test")
        .expiration(LocalDateTime.now())
        .codeType(CodeType.PASSWORD_RESET)
        .status(CodeStatus.ACTIVE)
        .build();
  }
}
