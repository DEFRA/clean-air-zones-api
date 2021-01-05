package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

public class ChargeCalculationRefreshLambdaInputTest {

  @Test
  public void shouldNotAcceptNullCorrelationId() {
    // given
    UUID correlationId = null;
    ChargeCalculationRefreshLambdaInput lambdaInput = ChargeCalculationRefreshLambdaInput.builder()
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
    UUID correlationId = UUID.randomUUID();

    // when
    ChargeCalculationRefreshLambdaInput lambdaInput = ChargeCalculationRefreshLambdaInput.builder()
        .correlationId(correlationId)
        .build();

    lambdaInput.validate();

    // then
    assertThat(lambdaInput.getCorrelationId()).isEqualTo(correlationId);
  }

  @Test
  public void shouldReturnProvidedValueForInvocationNumber() {
    // given
    Integer invocationNumber = RandomUtils.nextInt();
    UUID correlationId = UUID.randomUUID();

    // when
    ChargeCalculationRefreshLambdaInput lambdaInput = ChargeCalculationRefreshLambdaInput.builder()
        .correlationId(correlationId)
        .invocationNumber(invocationNumber)
        .build();

    // then
    assertThat(lambdaInput.getInvocationNumber()).isEqualTo(invocationNumber);
  }
}