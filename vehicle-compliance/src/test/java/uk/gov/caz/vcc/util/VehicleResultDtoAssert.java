package uk.gov.caz.vcc.util;

import java.util.List;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import uk.gov.caz.vcc.dto.VehicleResultDto;

public class VehicleResultDtoAssert extends
    AbstractAssert<VehicleResultDtoAssert, VehicleResultDto> {

  VehicleResultDtoAssert(VehicleResultDto actual) {
    super(actual, VehicleResultDtoAssert.class);
  }

  public static VehicleResultDtoAssert assertThat(List<VehicleResultDto> actual) {
    return new VehicleResultDtoAssert(actual.get(0));
  }

  public VehicleResultDtoAssert hasVrn(String expectedVrn) {
    Assertions.assertThat(actual.getVrn()).isEqualTo(expectedVrn);
    return this;
  }

  public VehicleResultDtoAssert hasMake(String expectedMake) {
    Assertions.assertThat(actual.getMake()).isEqualTo(expectedMake);
    return this;
  }

  public VehicleResultDtoAssert makeIsNull() {
    Assertions.assertThat(actual.getMake()).isNull();
    return this;
  }

  public VehicleResultDtoAssert hasModel(String expectedModel) {
    Assertions.assertThat(actual.getModel()).isEqualTo(expectedModel);
    return this;
  }

  public VehicleResultDtoAssert modelIsNull() {
    Assertions.assertThat(actual.getModel()).isNull();
    return this;
  }

  public VehicleResultDtoAssert hasColour(String expectedColour) {
    Assertions.assertThat(actual.getColour()).isEqualTo(expectedColour);
    return this;
  }

  public VehicleResultDtoAssert colourIsNull() {
    Assertions.assertThat(actual.getColour()).isNull();
    return this;
  }

  public VehicleResultDtoAssert hasTypeApproval(String expectedTypeApproval) {
    Assertions.assertThat(actual.getTypeApproval()).isEqualTo(expectedTypeApproval);
    return this;
  }

  public VehicleResultDtoAssert typeApprovalIsNull() {
    Assertions.assertThat(actual.getTypeApproval()).isNull();
    return this;
  }

  public VehicleResultDtoAssert hasStatus(String expectedStatus) {
    Assertions.assertThat(actual.getStatus()).isEqualTo(expectedStatus);
    return this;
  }

  public VehicleResultDtoAssert hasPaymentMethod(String expectedPaymentMethod) {
    if (expectedPaymentMethod == null) {
      Assertions.assertThat(actual.getPaymentMethod()).isNull();
    } else {
      Assertions.assertThat(actual.getPaymentMethod()).isEqualTo(expectedPaymentMethod);
    }
    return this;
  }

  public VehicleResultDtoAssert hasTariffCode(String expectedTariffCode) {
    Assertions.assertThat(actual.getTariffCode()).isEqualTo(expectedTariffCode);
    return this;
  }

  public VehicleResultDtoAssert tariffCodeIsNull() {
    Assertions.assertThat(actual.getTariffCode()).isNull();
    return this;
  }

  public VehicleResultDtoAssert hasExemptionCode(String expectedExemptionCode) {
    Assertions.assertThat(actual.getExemptionCode()).isEqualTo(expectedExemptionCode);
    return this;
  }

  public VehicleResultDtoAssert exemptionCodeIsNull() {
    Assertions.assertThat(actual.getExemptionCode()).isNull();
    return this;
  }

  public VehicleResultDtoAssert hasLicensingAuthority(List<String> expectedLicensingAuthority) {
    Assertions.assertThat(actual.getLicensingAuthority()).isEqualTo(expectedLicensingAuthority);
    return this;
  }

  public VehicleResultDtoAssert licensingAuthorityIsNull() {
    Assertions.assertThat(actual.getLicensingAuthority()).isNull();
    return this;
  }

  public VehicleResultDtoAssert isTaxiOrPhv() {
    Assertions.assertThat(actual.isTaxiOrPhv()).isTrue();
    return this;
  }

  public VehicleResultDtoAssert isNotTaxiOrPhv() {
    Assertions.assertThat(actual.isTaxiOrPhv()).isFalse();
    return this;
  }
}