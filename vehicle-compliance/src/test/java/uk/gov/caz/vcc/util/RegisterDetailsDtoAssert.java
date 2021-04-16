package uk.gov.caz.vcc.util;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import uk.gov.caz.vcc.dto.RegisterDetailsDto;

public class RegisterDetailsDtoAssert  extends
    AbstractAssert<RegisterDetailsDtoAssert, RegisterDetailsDto> {

  RegisterDetailsDtoAssert(RegisterDetailsDto actual) {
    super(actual, RegisterDetailsDtoAssert.class);
  }

  public static RegisterDetailsDtoAssert assertThat(RegisterDetailsDto actual) {
    return new RegisterDetailsDtoAssert(actual);
  }

  public RegisterDetailsDtoAssert isCompliant() {
    Assertions.assertThat(actual.isRegisterCompliant()).isTrue();
    return this;
  }

  public RegisterDetailsDtoAssert isNotCompliant() {
    Assertions.assertThat(actual.isRegisterCompliant()).isFalse();
    return this;
  }

  public RegisterDetailsDtoAssert isExempt() {
    Assertions.assertThat(actual.isRegisterExempt()).isTrue();
    return this;
  }

  public RegisterDetailsDtoAssert isNotExempt() {
    Assertions.assertThat(actual.isRegisterExempt()).isFalse();
    return this;
  }

  public RegisterDetailsDtoAssert isInRetrofit() {
    Assertions.assertThat(actual.isRegisteredRetrofit()).isTrue();
    return this;
  }

  public RegisterDetailsDtoAssert isNotInRetrofit() {
    Assertions.assertThat(actual.isRegisteredRetrofit()).isFalse();
    return this;
  }

  public RegisterDetailsDtoAssert isInMod() {
    Assertions.assertThat(actual.isRegisteredMod()).isTrue();
    return this;
  }

  public RegisterDetailsDtoAssert isNotInMod() {
    Assertions.assertThat(actual.isRegisteredMod()).isFalse();
    return this;
  }

  public RegisterDetailsDtoAssert isInGpw() {
    Assertions.assertThat(actual.isRegisteredGpw()).isTrue();
    return this;
  }

  public RegisterDetailsDtoAssert isNotInGpw() {
    Assertions.assertThat(actual.isRegisteredGpw()).isFalse();
    return this;
  }

  public RegisterDetailsDtoAssert isInNtr() {
    Assertions.assertThat(actual.isRegisteredNtr()).isTrue();
    return this;
  }

  public RegisterDetailsDtoAssert isNotInNtr() {
    Assertions.assertThat(actual.isRegisteredNtr()).isFalse();
    return this;
  }
}