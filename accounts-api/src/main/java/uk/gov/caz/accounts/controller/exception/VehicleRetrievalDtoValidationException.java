package uk.gov.caz.accounts.controller.exception;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class VehicleRetrievalDtoValidationException extends IllegalArgumentException {
  List<String> errorParams;
}