package uk.gov.caz.definitions.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Serializable entity representing data returned from remote data store.
 * 
 */
@MappedSuperclass
public class RemoteVehicleDataResponse implements Serializable {

  private static final long serialVersionUID = -977852507427471172L;

  @Id
  private String registrationNumber;

  private String colour;

  @JsonFormat(pattern = "yyyy-MM")
  private Date dateOfFirstRegistration;

  private String euroStatus;

  private String typeApproval;

  private Integer massInService;

  private String bodyType;

  private String make;

  private String model;

  private Integer revenueWeight;

  private Integer seatingCapacity;

  private Integer standingCapacity;

  private String taxClass;

  private String fuelType;

  public String getFuelType() {
    return fuelType;
  }

  public void setFuelType(String fuelType) {
    this.fuelType = fuelType;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public String getColour() {
    return colour;
  }

  public void setColour(String colour) {
    this.colour = colour;
  }

  public Date getDateOfFirstRegistration() {
    return dateOfFirstRegistration;
  }

  public void setDateOfFirstRegistration(Date dateOfFirstRegistration) {
    this.dateOfFirstRegistration = dateOfFirstRegistration;
  }

  /**
   * Return null when euro status is null, trimmed string otherwise.
   */
  public String getEuroStatus() {
    return (euroStatus == null) ? null : euroStatus.trim();
  }

  public void setEuroStatus(String euroStatus) {
    this.euroStatus = euroStatus;
  }

  public String getTypeApproval() {
    return typeApproval;
  }

  @JsonProperty("typeApprovalCategory")
  public void setTypeApproval(String typeApproval) {
    this.typeApproval = typeApproval;
  }

  public Integer getMassInService() {
    return massInService;
  }

  public void setMassInService(Integer massInService) {
    this.massInService = massInService;
  }

  public String getBodyType() {
    return (bodyType == null) ? null : bodyType.toLowerCase();
  }

  public void setBodyType(String bodyType) {
    this.bodyType = bodyType;
  }

  public String getMake() {
    return make;
  }

  public void setMake(String make) {
    this.make = make;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public Integer getRevenueWeight() {
    return revenueWeight;
  }

  public void setRevenueWeight(Integer grossWeight) {
    this.revenueWeight = grossWeight;
  }

  public Integer getSeatingCapacity() {
    return seatingCapacity;
  }

  public void setSeatingCapacity(Integer seatingCapacity) {
    this.seatingCapacity = seatingCapacity;
  }

  public Integer getStandingCapacity() {
    return standingCapacity;
  }

  public void setStandingCapacity(Integer standingCapacity) {
    this.standingCapacity = standingCapacity;
  }

  public String getTaxClass() {
    return (taxClass == null) ? null : taxClass.toLowerCase();
  }

  public void setTaxClass(String taxClass) {
    this.taxClass = taxClass;
  }

}