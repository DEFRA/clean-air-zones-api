package uk.gov.caz.taxiregister.model;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.Value;

@Value
public class LicenseDates implements Serializable {

  private static final long serialVersionUID = -3964245578530927140L;
  LocalDate start;
  LocalDate end;
}
