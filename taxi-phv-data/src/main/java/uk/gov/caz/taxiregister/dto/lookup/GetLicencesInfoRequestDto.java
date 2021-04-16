package uk.gov.caz.taxiregister.dto.lookup;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.Value;
import uk.gov.caz.taxiregister.controller.exception.PayloadValidationException;

@Value
public class GetLicencesInfoRequestDto {

  public static final int MAX_SIZE = 150;

  private static final int VRM_MAX_LENGTH = 15;
  private static final int VRM_MIN_LENGTH = 2;

  private static final Map<Function<GetLicencesInfoRequestDto, Boolean>, String> validators =
      ImmutableMap.<Function<GetLicencesInfoRequestDto, Boolean>, String>builder()
          .put(vrmsNotNull(), "'vrms' cannot be null or empty")
          .put(notTooManyVrms(), "'vrms' can have up to " + MAX_SIZE + " elements")
          .put(vrmsHaveCorrectLength(), "Incorrect length of a vrn detected")
          .build();

  Set<String> vrms;

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new PayloadValidationException(message);
      }
    });
  }

  /**
   * Returns a lambda that verifies if 'vrms' is not null.
   */
  private static Function<GetLicencesInfoRequestDto, Boolean> vrmsNotNull() {
    return request -> Objects.nonNull(request.getVrms());
  }

  /**
   * Returns a lambda that verifies if 'vrms' has correct size.
   */
  private static Function<GetLicencesInfoRequestDto, Boolean> notTooManyVrms() {
    return request -> request.getVrms().size() <= MAX_SIZE;
  }

  /**
   * Returns a lambda that verifies if 'vrms' has correct size.
   */
  private static Function<GetLicencesInfoRequestDto, Boolean> vrmsHaveCorrectLength() {
    return request -> request.getVrms()
        .stream()
        .allMatch(vrm -> vrm.length() >= VRM_MIN_LENGTH && vrm.length() <= VRM_MAX_LENGTH);
  }
}
