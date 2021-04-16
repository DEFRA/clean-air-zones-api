package uk.gov.caz.definitions.domain;

public enum VehicleType {
  PRIVATE_CAR("Car"),
  VAN("Van"),
  MINIBUS("Minibus"),
  TAXI_OR_PHV("Taxi"),
  BUS("Bus"),
  COACH("Coach"),
  MOTORCYCLE("Motorcycle"),
  HGV("Heavy Goods Vehicle"),
  AGRICULTURAL("Agricultural Vehicle");
  
  private final String text;
  
  VehicleType(String text) {
    this.text = text;
  }
  
  @Override
  public String toString() {
    return this.text;
  }
}