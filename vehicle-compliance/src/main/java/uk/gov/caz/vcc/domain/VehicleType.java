package uk.gov.caz.vcc.domain;

public enum VehicleType {
  PRIVATE_CAR("Car"),
  LARGE_VAN("Large Van"),
  SMALL_VAN("Small Van"),
  MINIBUS("Minibus"),
  TAXI_OR_PHV("Taxi"),
  BUS("Bus"),
  COACH("Coach"),
  MOTORCYCLE("Motorcycle"),
  HGV("Heavy Goods Vehicle"),
  AGRICULTURAL("Agricultural Vehicle");
  
  private final String text;
  
  private VehicleType(String text) {
    this.text = text;
  }
  
  @Override
  public String toString() {
    return this.text;
  }
}
