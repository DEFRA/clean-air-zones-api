package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Value object that represents an input request for setting permissions or a new name for the user.
 */
@Value
@Builder
public class UpdateUserRequestDto {

  @ApiModelProperty(value = "${swagger.model.descriptions.users.permissions}")
  List<String> permissions;

  @ApiModelProperty(value = "${swagger.model.descriptions.users.name}")
  String name;

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

  private static final Map<Function<UpdateUserRequestDto, Boolean>, String>
      validators =
      MapPreservingOrderBuilder.<Function<UpdateUserRequestDto, Boolean>, String>builder()
          .put(eitherPermissionsOrNameIsSet(), "Either 'permissions' or 'name' should be set")
          .build();

  private static Function<UpdateUserRequestDto, Boolean> eitherPermissionsOrNameIsSet() {
    return request -> !Objects.isNull(request.permissions) || !Objects.isNull(request.name);
  }

  /**
   * Verifies if this dto is carrying information about permissions to be changed.
   * @return information on the fact if we are changing permissions.
   */
  public boolean isChangingPermissions() {
    return !Objects.isNull(permissions);
  }

  /**
   * Verifies if this dto is carrying information about username to be changed.
   * @return information on the fact if we are changing username.
   */
  public boolean isChangingName() {
    return !Objects.isNull(name);
  }

}
