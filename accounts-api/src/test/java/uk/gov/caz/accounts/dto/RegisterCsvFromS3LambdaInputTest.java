package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterCsvFromS3LambdaInputTest {

  @Test
  public void shouldReturnTrueForShouldSendEmailsUponSuccessfulChargeCalculationIfNull() {
    // given
    Boolean shouldSendEmailsUponSuccessfulChargeCalculation = null;

    // when
    RegisterCsvFromS3LambdaInput lambdaInput = RegisterCsvFromS3LambdaInput.builder()
        .shouldSendEmailsUponSuccessfulChargeCalculation(
            shouldSendEmailsUponSuccessfulChargeCalculation)
        .build();

    // then
    assertThat(lambdaInput.shouldSendEmailsUponSuccessfulChargeCalculation()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans ={true, false})
  public void shouldReturnProvidedValueForShouldSendEmailsUponSuccessfulChargeCalculationFlag(
      boolean shouldSendEmailsUponSuccessfulChargeCalculation) {
    // given

    // when
    RegisterCsvFromS3LambdaInput lambdaInput = RegisterCsvFromS3LambdaInput.builder()
        .shouldSendEmailsUponSuccessfulChargeCalculation(
            shouldSendEmailsUponSuccessfulChargeCalculation)
        .build();

    // then
    assertThat(lambdaInput.shouldSendEmailsUponSuccessfulChargeCalculation())
        .isEqualTo(shouldSendEmailsUponSuccessfulChargeCalculation);
  }
}