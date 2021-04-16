package uk.gov.caz.whitelist.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import uk.gov.caz.whitelist.dto.validation.ActionValidator;
import uk.gov.caz.whitelist.dto.validation.CategoryValidator;
import uk.gov.caz.whitelist.dto.validation.EmailValidator;
import uk.gov.caz.whitelist.dto.validation.ManufacturerValidator;
import uk.gov.caz.whitelist.dto.validation.ReasonValidator;
import uk.gov.caz.whitelist.dto.validation.VrnValidator;
import uk.gov.caz.whitelist.dto.validation.WhitelistedVehicleValidator;
import uk.gov.caz.whitelist.model.CategoryType;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.model.WhitelistVehicle.WhitelistVehicleBuilder;

/**
 * DTO class that holds Whitelisted Vehicle. It is used to transport CSV fields that eventually will
 * be mapped to domain object.
 */
@Value
@Builder(toBuilder = true)
public class WhitelistedVehicleDto {

  private static final List<WhitelistedVehicleValidator> VALIDATORS = Arrays.asList(
      new VrnValidator(),
      new ActionValidator(),
      new CategoryValidator(),
      new ReasonValidator(),
      new ManufacturerValidator(),
      new EmailValidator()
  );

  String vrn;

  String reason;

  @Default
  Optional<String> manufacturer = Optional.empty();

  String category;

  String action;

  int lineNumber;

  boolean exempt;

  boolean compliant;

  String email;

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

  /**
   * Validates this instance with an additional validator.
   *
   * @return a list of validation errors if there are any. An empty list is returned if validation
   *     succeeds.
   */
  public List<ValidationError> validateWithAdditionaValidator(
      WhitelistedVehicleValidator additionalValidator) {
    List<WhitelistedVehicleValidator> validators = new ArrayList<>(VALIDATORS);
    validators.add(additionalValidator);
    return validators.stream()
        .map(validator -> validator.validate(this))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  /**
   * Helper method to map request to dto.
   *
   * @param dto {@link WhitelistedVehicleRequestDto}
   * @param action String value, one of C, U, D.
   * @return {@link WhitelistedVehicleDto}
   */
  public static WhitelistedVehicleDto from(WhitelistedVehicleRequestDto dto, String action) {
    String vrn = dto.getVrn();
    return WhitelistedVehicleDto.builder()
        .vrn(vrn)
        .category(dto.getCategory())
        .reason(dto.getReasonUpdated())
        .email(dto.getEmail())
        .manufacturer(Optional.ofNullable(dto.getManufacturer()))
        .action(action)
        .lineNumber(0)
        .build();
  }

  /**
   * Helper method to map test fixtures to dto.
   *
   * @param dto {@link TestFixturesVehicleDto}
   * @return {@link WhitelistedVehicleDto}
   */
  public static WhitelistedVehicleDto from(TestFixturesVehicleDto dto) {
    return WhitelistedVehicleDto.builder()
        .vrn(dto.getVrn())
        .category(dto.getCategory())
        .reason(dto.getReasonUpdated())
        .manufacturer(Optional.ofNullable(dto.getManufacturer()))
        .action(dto.getAction())
        .build();
  }

  /**
   * Map {@link WhitelistedVehicleDto} to {@link WhitelistVehicle}.
   *
   * @return {@link WhitelistVehicle}
   */
  public WhitelistVehicle mapToWhitelistVehicle(UUID uploaderId) {
    return mapToWhitelistVehicle(uploaderId, this.email);
  }

  /**
   * Map {@link WhitelistedVehicleDto} to {@link WhitelistVehicle}.
   *
   * @return {@link WhitelistVehicle}
   */
  public WhitelistVehicle mapToWhitelistVehicle(UUID uploaderId, String email) {
    WhitelistVehicleBuilder whitelistVehicleBuilder = WhitelistVehicle.builder()
        .vrn(vrn.toUpperCase())
        .reasonUpdated(reason)
        .manufacturer(manufacturer.orElse(null))
        .uploaderId(uploaderId)
        .uploaderEmail(email)
        .updateTimestamp(LocalDateTime.now());
    CategoryType.fromCategory(category).ifPresent(categoryType -> {
      whitelistVehicleBuilder.category(categoryType.getCategory());
      whitelistVehicleBuilder.compliant(categoryType.isCompliant());
      whitelistVehicleBuilder.exempt(categoryType.isExempt());
    });
    return whitelistVehicleBuilder.build();
  }
}
