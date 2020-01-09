package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.testutils.TestObjects;

@ExtendWith(MockitoExtension.class)
class SuccessRegisterResultTest {

  @Test
  public void shouldReturnFalseForIsSuccess() {
    // when
    RegisterResult result = SuccessRegisterResult
        .with(TestObjects.LicensingAuthorities.existingAsSingleton());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void shouldReturnEmptyAffectedLicensingAuthorities() {
    // when
    RegisterResult result = SuccessRegisterResult
        .with(TestObjects.LicensingAuthorities.existingAsSingleton());

    // then
    assertThat(result.getValidationErrors()).isEmpty();
  }
}