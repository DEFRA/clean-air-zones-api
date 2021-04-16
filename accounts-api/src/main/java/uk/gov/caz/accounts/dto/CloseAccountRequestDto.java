package uk.gov.caz.accounts.dto;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.model.AccountClosureReason;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents incoming JSON payload for inactivate an account.
 */
@Value
@Builder
public class CloseAccountRequestDto {

  /**
   * A reason of inactivate account.
   */
  @ApiModelProperty(value = "${swagger.operations.accounts.inactivate.reason}")
  String reason;

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<CloseAccountRequestDto, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<CloseAccountRequestDto, Boolean>, String>builder()
          .put(reasonIsNotBlank(), "Reason cannot be null or empty.")
          .put(reasonIsValid(), "Reason value is not on the list.")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Returns a lambda that verifies if 'reason' is not null or empty.
   */
  private static Function<CloseAccountRequestDto, Boolean> reasonIsNotBlank() {
    return request -> !Strings.isNullOrEmpty(request.reason);
  }

  /**
   * Returns a lambda that verifies if 'reason' value on the list.
   */
  private static Function<CloseAccountRequestDto, Boolean> reasonIsValid() {
    return request -> EnumUtils.isValidEnum(AccountClosureReason.class, request.getReason());
  }
}
