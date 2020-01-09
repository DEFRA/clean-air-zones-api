package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.testutils.TestObjects;

@ExtendWith(MockitoExtension.class)
class FailureRegisterResultTest {

  @Test
  public void shouldReturnFalseForIsSuccess() {
    // when
    RegisterResult result = FailureRegisterResult
        .with(TestObjects.MODIFIED_REGISTER_JOB_VALIDATION_ERRORS);

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void shouldReturnEmptyAffectedLicensingAuthorities() {
    // when
    RegisterResult result = FailureRegisterResult
        .with(TestObjects.MODIFIED_REGISTER_JOB_VALIDATION_ERRORS);

    // then
    assertThat(result.getAffectedLicensingAuthorities()).isEmpty();
  }
}