package uk.gov.caz.accounts.dto;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.dto.validation.AccountVehicleValidator;
import uk.gov.caz.accounts.dto.validation.VrnValidator;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

/**
 * DTO class that holds AccountVehicle.
 * It is used to transport CSV fields that eventually will be mapped to domain objects.
 */
@Value
@Builder
public class AccountVehicleDto {

  private static final List<AccountVehicleValidator> VALIDATORS = ImmutableList.of(
      new VrnValidator()
  );

  String vrn;

  int lineNumber;

  /**
   * Validates this instance.
   *
   * @return a list of validation errors if there are any. An empty list is returned if validation
   *     succeeds.
   */
  public List<ValidationError> validate() {
    return VALIDATORS.stream()
        .map(validator -> validator.validate(this))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
