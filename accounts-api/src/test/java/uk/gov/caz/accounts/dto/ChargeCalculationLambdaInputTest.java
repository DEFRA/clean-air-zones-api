package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ChargeCalculationLambdaInputTest {

  @Test
  public void shouldNotAcceptNullAccountId() {
    // given
    Integer jobId = RandomUtils.nextInt();
    UUID accountId = null;
    UUID correlationId = UUID.randomUUID();
    ChargeCalculationLambdaInput lambdaInput = ChargeCalculationLambdaInput.builder()
        .accountId(accountId)
        .jobId(jobId)
        .correlationId(correlationId)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class);
    assertThat(throwable).hasMessageContaining("accountId");
  }

  @Test
  public void shouldNotAcceptNullJobId() {
    // given
    Integer jobId = null;
    UUID accountId = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();
    ChargeCalculationLambdaInput lambdaInput = ChargeCalculationLambdaInput.builder()
        .accountId(accountId)
        .jobId(jobId)
        .correlationId(correlationId)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class);
    assertThat(throwable).hasMessageContaining("jobId");
  }

  @Test
  public void shouldNotAcceptNullCorrelationId() {
    // given
    Integer jobId = RandomUtils.nextInt();
    UUID accountId = UUID.randomUUID();
    UUID correlationId = null;
    ChargeCalculationLambdaInput lambdaInput = ChargeCalculationLambdaInput.builder()
        .accountId(accountId)
        .jobId(jobId)
        .correlationId(correlationId)
        .build();

    // when
    Throwable throwable = catchThrowable(lambdaInput::validate);

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class);
    assertThat(throwable).hasMessageContaining("correlationId");
  }

  @Test
  public void shouldCreateValidObject() {
    // given
    Integer jobId = RandomUtils.nextInt();
    UUID accountId = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();

    // when
    ChargeCalculationLambdaInput lambdaInput = ChargeCalculationLambdaInput.builder()
        .accountId(accountId)
        .correlationId(correlationId)
        .jobId(jobId)
        .build();

    lambdaInput.validate();

    // then
    assertThat(lambdaInput.getJobId()).isEqualTo(jobId);
    assertThat(lambdaInput.getAccountId()).isEqualTo(accountId);
  }

  @Test
  public void shouldReturnTrueForShouldSendEmailsUponSuccessfulJobCompletionIfNull() {
    // given
    Integer jobId = RandomUtils.nextInt();
    UUID accountId = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();

    // when
    ChargeCalculationLambdaInput lambdaInput = ChargeCalculationLambdaInput.builder()
        .accountId(accountId)
        .correlationId(correlationId)
        .jobId(jobId)
        .shouldSendEmailsUponSuccessfulJobCompletion(null)
        .build();

    // then
    assertThat(lambdaInput.shouldSendEmailsUponSuccessfulJobCompletion()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans ={true, false})
  public void shouldReturnProvidedValueForShouldSendEmailsUponSuccessfulJobCompletionFlag(
      boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    // given
    Integer jobId = RandomUtils.nextInt();
    UUID accountId = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();

    // when
    ChargeCalculationLambdaInput lambdaInput = ChargeCalculationLambdaInput.builder()
        .accountId(accountId)
        .correlationId(correlationId)
        .jobId(jobId)
        .shouldSendEmailsUponSuccessfulJobCompletion(shouldSendEmailsUponSuccessfulJobCompletion)
        .build();

    // then
    assertThat(lambdaInput.shouldSendEmailsUponSuccessfulJobCompletion())
        .isEqualTo(shouldSendEmailsUponSuccessfulJobCompletion);
  }
}
