package uk.gov.caz.accounts.dto;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

/**
 * Class that represents payload for user validation.
 */
@Value
@Builder
public class UserValidationRequest {
  String email;
  String name;

  /**
   * Method that validates if fields have required values.
   */
  public UserValidationRequest validate() {
    if (StringUtils.isBlank(email)) {
      throw new InvalidRequestPayloadException("Email cannot be blank");
    }

    return this;
  }
}
