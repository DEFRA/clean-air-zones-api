package uk.gov.caz.accounts.service.generatecsv;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.VccsRepository;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;

/**
 * Generates content for csv file.
 */
@Component
@AllArgsConstructor
public class CsvContentGenerator {

  private static final String LIVE = "(Live)";
  private static final String UPCOMING = "(Upcoming)";
  private static final String UNDETERMINED = "Undetermined";
  private static final String NO_CHARGE = "No charge";
  private static final String COMMA = ",";
  private final VccsRepository vccsRepository;
  private final AccountVehicleRepository accountVehicleRepository;

  /**
   * Generate header row.
   *
   * @return {@link String} with header row.
   */
  public String generateHeaders() {
    StringBuilder headerRecord = new StringBuilder("Number plate,Vehicle Type");
    getSortedCazNamesAndSuffix()
        .forEach((key, value) -> headerRecord.append(COMMA).append(key).append(" ").append(value));
    return headerRecord.toString();
  }

  /**
   * Generate {@link List} of String[] which contains csv rows.
   *
   * @param accountId ID of Account/Fleet.
   * @return {@link List} of String[] which contains csv rows.
   */
  public List<String[]> generateCsvRows(UUID accountId) {
    String headers = generateHeaders();

    List<String[]> csvRows = new ArrayList<>();
    List<AccountVehicle> vehiclesWithChargeability = getVehiclesWithChargeability(accountId);
    // adding header record
    csvRows.add(new String[]{headers});
    Map<String, UUID> sortedCazNamesAndIds = getSortedNamesAndIds();

    for (AccountVehicle accountVehicle : vehiclesWithChargeability) {
      StringBuilder row = createCsvRow(accountVehicle);

      List<VehicleChargeability> vehicleChargeability = accountVehicle.getVehicleChargeability();
      List<UUID> chargeabilityCazIds = getCazIds(vehicleChargeability);
      for (Entry entry : sortedCazNamesAndIds.entrySet()) {
        UUID uuid = getCazId(chargeabilityCazIds, entry);
        if (uuid == null) {
          row.append(COMMA);
        } else {
          addCharges(row, vehicleChargeability, entry);
        }
      }
      csvRows.add(new String[]{row.toString()});
    }
    return csvRows;
  }

  private List<UUID> getCazIds(List<VehicleChargeability> vehicleChargeability) {
    return vehicleChargeability
        .stream()
        .map(VehicleChargeability::getCazId)
        .collect(Collectors.toList());
  }

  private void addCharges(StringBuilder row, List<VehicleChargeability> vehicleChargeability,
      Entry entry) {
    for (VehicleChargeability vc : vehicleChargeability) {
      UUID chargeabilityCazId = vc.getCazId();
      if (chargeabilityCazId.equals(entry.getValue())) {
        String charge = getCharge(vc);
        row.append(COMMA).append(charge);
      }
    }
  }

  private StringBuilder createCsvRow(AccountVehicle accountVehicle) {
    StringBuilder row = new StringBuilder()
        .append(accountVehicle.getVrn())
        .append(COMMA);
    if (accountVehicle.getCazVehicleType() == null) {
      row.append(UNDETERMINED);
    } else {
      row.append(accountVehicle.getCazVehicleType());
    }
    return row;
  }

  private UUID getCazId(List<UUID> chargeabilityCazs, Entry entry) {
    return chargeabilityCazs
        .stream()
        .filter(cazId -> cazId.equals(entry.getValue()))
        .findFirst()
        .orElse(null);
  }

  private List<CleanAirZoneDto> fetchCazes() {
    return vccsRepository
        .findCleanAirZonesSync()
        .body()
        .getCleanAirZones();
  }

  private Map<String, UUID> getSortedNamesAndIds() {
    Map<String, UUID> cazNamesAndIds = fetchCazes()
        .stream()
        .collect(toMap(CleanAirZoneDto::getName, CleanAirZoneDto::getCleanAirZoneId));
    return new TreeMap<>(cazNamesAndIds);
  }

  private Map<String, String> getSortedCazNamesAndSuffix() {
    Map<String, String> cazNamesWithSuffix = fetchCazes()
        .stream()
        .collect(toMap(CleanAirZoneDto::getName, this::mapDateToSuffix));

    return new TreeMap<>(cazNamesWithSuffix);
  }

  private String getCharge(VehicleChargeability vc) {
    if (vc.getCharge() == null) {
      return UNDETERMINED;
    }
    if (isChargeZero(vc)) {
      return NO_CHARGE;
    }
    return getChargeWithStrippedZeros(vc);
  }

  private String getChargeWithStrippedZeros(VehicleChargeability vc) {
    return vc.getCharge().stripTrailingZeros().toPlainString();
  }

  private boolean isChargeZero(VehicleChargeability vc) {
    return vc.getCharge().stripTrailingZeros().equals(BigDecimal.ZERO);
  }

  private List<AccountVehicle> getVehiclesWithChargeability(UUID accountId) {
    return accountVehicleRepository
        .findByAccountIdWithChargeabilityAndOrderByVrnAsc(accountId);
  }

  private String mapDateToSuffix(CleanAirZoneDto cleanAirZoneDto) {
    LocalDate activeChargeStartDate = LocalDate
        .parse(cleanAirZoneDto.getActiveChargeStartDate(), DateTimeFormatter.ISO_DATE);
    if (activeChargeStartDate.isBefore(LocalDate.now())) {
      return LIVE;
    }
    return UPCOMING;
  }
}
