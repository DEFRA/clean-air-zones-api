package uk.gov.caz.vcc.dto;

/**
 * Returns one of the following values: • exempt – The VRN is registered to a vehicle that meets one
 * or more national exemption criteria • compliant – The VRN is registered to a vehicle that is
 * compliant with the CAZ framework. • notCompliantPaid – The vehicle is eligible to be  charged
 * and has paid the charge in advance. • notCompliantNotPaid – The vehicle is eligible to be charged
 * and has not yet paid the charge in advance. • unrecognisedPaid – The VRN is not recognised by
 * DVLA but has paid the charge in advance. An unrecognised vehicle may indicate: (1) the VRN
 * corresponds to a non-UK vehicle; or, (2) the VRN has been incorrectly recognised by ANPR
 * infrastructure. • unrecognisedNotPaid – The VRN is not recognised by DVLA and has not yet paid
 * the charge. An unrecognised vehicle may indicate: (1) the VRN corresponds to a non-UK vehicle;
 * or, (2) the VRN has been incorrectly recognised by ANPR infrastructure.
 */
public enum VehicleComplianceStatus {

  EXEMPT("exempt"),
  COMPLIANT("compliant"),
  NOT_COMPLIANT_PAID("notCompliantPaid"),
  NOT_COMPLIANT_NOT_PAID("notCompliantNotPaid"),
  UNRECOGNISED_PAID("unrecognisedPaid"),
  UNRECOGNISED_NOT_PAID("unrecognisedNotPaid");

  private final String status;

  VehicleComplianceStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return this.status;
  }
}