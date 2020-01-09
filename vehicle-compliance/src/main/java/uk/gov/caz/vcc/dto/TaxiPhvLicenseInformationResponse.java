package uk.gov.caz.vcc.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaxiPhvLicenseInformationResponse implements Serializable {

  private static final long serialVersionUID = 74042876269358105L;

  boolean active;

  Boolean wheelchairAccessible;

  private List<String> licensingAuthoritiesNames;
}