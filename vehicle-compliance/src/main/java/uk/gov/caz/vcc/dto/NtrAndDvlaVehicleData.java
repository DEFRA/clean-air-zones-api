package uk.gov.caz.vcc.dto;

import lombok.Value;

@Value
public class NtrAndDvlaVehicleData {
  SingleDvlaVehicleData dvlaVehicleData;
  SingleLicenceData ntrVehicleData;
}
