package uk.gov.caz.accounts.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.model.VehiclesWithAnyUndeterminedChargeabilityFlagData;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;

/**
 * Converts {@code Page<AccountVehicle>} to {@link VehiclesResponseDto}.
 */
@UtilityClass
public class VehiclesResponseDtoConverter {

  /**
   * Converts {@link VehiclesWithAnyUndeterminedChargeabilityFlagData} to {@link
   * VehiclesResponseDto}.
   */
  public static VehiclesResponseDto toVehiclesResponseDto(
      VehiclesWithAnyUndeterminedChargeabilityFlagData data, Optional<String> cazId) {
    Page<AccountVehicle> page = data.getVehicles();
    return VehiclesResponseDto.builder()
        .vehicles(toVehiclesResponse(page, cazId))
        .totalVehiclesCount(page.getTotalElements())
        .pageCount(page.getTotalPages())
        .anyUndeterminedVehicles(data.containsAnyUndeterminedVehicles())
        .build();
  }

  /**
   * Converts {@code Page<AccountVehicle>} to {@code List<VehicleWithCharges>}.
   */
  private List<VehicleWithCharges> toVehiclesResponse(Page<AccountVehicle> page,
      Optional<String> cazId) {
    return page.get()
        .map(accountVehicle ->
            VehiclesResponseDtoConverter.toVehicleResponse(accountVehicle, cazId))
        .collect(Collectors.toList());
  }

  /**
   * Converts {@link AccountVehicle} to {@link VehicleWithCharges}.
   */
  public static VehicleWithCharges toVehicleResponse(AccountVehicle accountVehicle,
      Optional<String> cazId) {
    List<VehicleChargeability> vehicleChargeability = accountVehicle.getVehicleChargeability();
    return VehicleWithCharges.builder()
        .vrn(accountVehicle.getVrn())
        .vehicleType(accountVehicle.getCazVehicleType())
        .exempt(isExempt(vehicleChargeability))
        .retrofitted(isRetrofitted(vehicleChargeability))
        .cachedCharges(toCachedCharges(vehicleChargeability, cazId))
        .build();
  }

  /**
   * Converts {@code List<VehicleChargeability>} to {@code List<VehicleCharge>}.
   */
  private List<VehicleCharge> toCachedCharges(List<VehicleChargeability> vehicleChargeability,
      Optional<String> cazId) {

    if (cazId.isPresent()) {
      UUID cazIdValue = UUID.fromString(cazId.get());
      return vehicleChargeability.stream()
          .filter(element -> element.getCazId().equals(cazIdValue))
          .map(VehiclesResponseDtoConverter::toVehicleCharge)
          .collect(Collectors.toList());
    }

    return vehicleChargeability.stream()
        .map(VehiclesResponseDtoConverter::toVehicleCharge)
        .collect(Collectors.toList());
  }

  /**
   * Converts {@link VehicleChargeability} to {@link VehicleCharge}.
   */
  private VehicleCharge toVehicleCharge(VehicleChargeability vc) {
    return VehicleCharge.builder()
        .cazId(vc.getCazId())
        .charge(vc.getCharge())
        .tariffCode(vc.getTariffCode())
        .build();
  }

  /**
   * Returns {@code true} if any of {@code vehicleChargeability} has {@code isExempt} flag set to
   * true, false otherwise.
   */
  private boolean isExempt(List<VehicleChargeability> vehicleChargeability) {
    return vehicleChargeability.stream()
        .anyMatch(VehicleChargeability::isExempt);
  }

  /**
   * Returns {@code true} if any of {@code vehicleChargeability} has {@code isRetrofitted} flag set
   * to true, false otherwise.
   */
  private boolean isRetrofitted(List<VehicleChargeability> vehicleChargeability) {
    return vehicleChargeability.stream()
        .anyMatch(VehicleChargeability::isRetrofitted);
  }
}
