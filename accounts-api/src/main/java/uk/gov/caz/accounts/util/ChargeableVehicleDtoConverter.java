package uk.gov.caz.accounts.util;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;

/**
 * Converts {@code List<AccountVehicle> accountVehicles} to {@link ChargeableVehiclesResponseDto}.
 */
@UtilityClass
public class ChargeableVehicleDtoConverter {

  /**
   * Converts {@code List<AccountVehicle> accountVehicles} to {@link ChargeableVehiclesResponseDto}.
   */
  public static ChargeableVehiclesResponseDto toChargeableVehiclesResponseDto(
      List<AccountVehicle> accountVehicles) {
    return ChargeableVehiclesResponseDto.builder()
        .vehicles(toVehiclesResponse(accountVehicles))
        .totalVehiclesCount(accountVehicles.size())
        .build();
  }

  /**
   * Converts {@code List<AccountVehicle> accountVehicles} to {@code List<VehicleWithCharges>}.
   */
  private List<VehicleWithCharges> toVehiclesResponse(List<AccountVehicle> accountVehicles) {
    return accountVehicles.stream()
        .map(VehiclesResponseDtoConverter::toVehicleResponse)
        .collect(Collectors.toList());
  }
}