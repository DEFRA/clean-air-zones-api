package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.repository.IdentityProvider;

@IntegrationTest
class ProcessInactiveUsersServiceTestIT {

  private static final UUID EXISTING_ACCOUNT_ID = UUID
      .fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");

  @Autowired
  private IdentityProvider identityProvider;

  @Autowired
  private ProcessInactiveUsersService processInactiveUsersService;

  @Autowired
  private AmazonSQS sqsClient;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataSource dataSource;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @BeforeEach
  public void createEmailQueue() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }

  @AfterEach
  public void deleteQueue() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  @Nested
  class WhenAccountWasInactiveFor165Days {

    @BeforeEach
    public void setUpDatabaseWithAccountInactiveFor165Days() {
      executeSqlFrom("data/sql/create-account-inactive-for-165-days.sql");
    }

    @AfterEach
    public void cleanDatabase() {
      executeSqlFrom("data/sql/delete-user-data.sql");
    }

    @Test
    public void shouldSendReminderEmail() {
      // given
      createUserInIdentityProvider();

      // when
      processInactiveUsersService.execute();

      // then
      assertThatAccountIsStillActive();
      assertThatNoUserIsRemovedFromCognito();
      assertThatNotificationIsSent();
    }

    private void assertThatAccountIsStillActive() {
      int inactiveAccountsCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account",
              "inactivation_tstamp IS NULL");
      assertThat(inactiveAccountsCount).isEqualTo(1);
    }

    private void assertThatNoUserIsRemovedFromCognito() {
      assertThat(identityProvider.checkIfUserExists("owner@email.com")).isTrue();
    }
  }

  @Nested
  class WhenAccountWasInactiveFor180Days {

    @BeforeEach
    public void setUpDatabaseWithAccountInactiveFor180Days() {
      executeSqlFrom("data/sql/create-account-inactive-for-180-days.sql");
    }

    @AfterEach
    public void cleanDatabase() {
      executeSqlFrom("data/sql/delete-user-data.sql");
    }

    @Test
    public void shouldInactivateAccountOn180thDay() {
      // given
      createUserInIdentityProvider();

      // when
      processInactiveUsersService.execute();

      // then
      assertThatAccountHasInactivationTimestampSet();
      assertThatUsersAreRemovedFromCognito();
      assertThatUsersHaveNullifiedIdentityProviderId();
      assertThatNotificationIsSent();
    }

    private void assertThatAccountHasInactivationTimestampSet() {
      int inactiveAccountsCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account",
              "inactivation_tstamp IS NOT NULL");
      assertThat(inactiveAccountsCount).isEqualTo(1);
    }

    private void assertThatUsersAreRemovedFromCognito() {
      assertThat(identityProvider.checkIfUserExists("owner@email.com")).isFalse();
    }

    private void assertThatUsersHaveNullifiedIdentityProviderId() {
      int usersWithNullifiedUserIdCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account_user", "user_id IS NULL");
      assertThat(usersWithNullifiedUserIdCount).isEqualTo(1);
    }
  }
  
  @Nested
  class WhenAccountWasInactiveFor181Days {

    @BeforeEach
    public void setUpDatabaseWithAccountInactiveFor181Days() {
      executeSqlFrom("data/sql/create-account-inactive-for-181-days.sql");
    }

    @AfterEach
    public void cleanDatabase() {
      executeSqlFrom("data/sql/delete-user-data.sql");
    }

    @Test
    public void shouldInactivateAccountOn181thDay() {
      // given
      createUserInIdentityProvider();

      // when
      processInactiveUsersService.execute();

      // then
      assertThatAccountHasInactivationTimestampSet();
      assertThatUsersAreRemovedFromCognito();
      assertThatUsersHaveNullifiedIdentityProviderId();
      assertThatNotificationIsSent();
    }

    private void assertThatAccountHasInactivationTimestampSet() {
      int inactiveAccountsCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account",
              "inactivation_tstamp IS NOT NULL");
      assertThat(inactiveAccountsCount).isEqualTo(1);
    }

    private void assertThatUsersAreRemovedFromCognito() {
      assertThat(identityProvider.checkIfUserExists("owner@email.com")).isFalse();
    }

    private void assertThatUsersHaveNullifiedIdentityProviderId() {
      int usersWithNullifiedUserIdCount = JdbcTestUtils
          .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account_user", "user_id IS NULL");
      assertThat(usersWithNullifiedUserIdCount).isEqualTo(1);
    }
  }
  
  private void createUserInIdentityProvider() {
    identityProvider.createAdminUser(
        UUID.fromString("d2b55341-551e-498d-a7be-a6e7f8639161"), "owner@email.com",
        "p4ssw0rd123456789");
  }

  @SneakyThrows
  private void assertThatNotificationIsSent() {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).isNotEmpty();
    String sendEmailBody = messages.iterator().next().getBody();
    JSONObject jsonEmailBody = new JSONObject(sendEmailBody);
    JSONObject jsonEmailVariables = new JSONObject(jsonEmailBody.get("personalisation").toString());
    String organisationName = jsonEmailVariables.getString("organisation");
    assertThat(organisationName).isEqualTo("test");
  }

  private List<Message> receiveSqsMessages() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageResult receiveMessageResult = sqsClient
        .receiveMessage(queueUrlResult.getQueueUrl());
    List<Message> messages = receiveMessageResult.getMessages();
    for (Message message : messages) {
      sqsClient.deleteMessage(queueUrlResult.getQueueUrl(), message.getReceiptHandle());
    }
    return messages;
  }
}
