package uk.gov.caz.vcc.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import uk.gov.caz.vcc.dto.InformationUrlsDto;

/**
 * Domain object representation of tariff details inclusive of rates, informational
 * urls etc.
 *
 */
public class TariffDetails implements Serializable {

  private static final long serialVersionUID = -2417542755465759599L;

  private UUID cazId;

  private String name;

  private CazClass tariffClass;

  private String chargeIdentifier;

  private boolean chargesMotorcycles;

  private List<VehicleTypeCharge> rates;

  private InformationUrlsDto informationUrls;
  
  boolean disabledTaxClassChargeable;

  public UUID getCazId() {
    return cazId;
  }

  public void setCazId(UUID cazId) {
    this.cazId = cazId;
  }

  public CazClass getCazClass() {
    return tariffClass;
  }

  public void setTariff(CazClass tariff) {
    this.tariffClass = tariff;
  }

  public boolean isChargesMotorcycles() {
    return chargesMotorcycles;
  }

  public void setChargesMotorcycles(boolean chargesMotorcycles) {
    this.chargesMotorcycles = chargesMotorcycles;
  }

  public List<VehicleTypeCharge> getRates() {
    return rates;
  }

  public void setRates(List<VehicleTypeCharge> rates) {
    this.rates = rates;
  }

  public InformationUrlsDto getInformationUrls() {
    return informationUrls;
  }

  public void setInformationUrls(InformationUrlsDto informationUrls) {
    this.informationUrls = informationUrls;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getChargeIdentifier() {
    return chargeIdentifier;
  }

  public void setChargeIdentifier(String chargeIdentifier) {
    this.chargeIdentifier = chargeIdentifier;
  }
  
  public boolean isDisabledTaxClassChargeable() {
    return disabledTaxClassChargeable;
  }

  public void setdisabledTaxClassChargeable(boolean disabledTaxClassChargeable) {
    this.disabledTaxClassChargeable = disabledTaxClassChargeable;
  }
  
}
