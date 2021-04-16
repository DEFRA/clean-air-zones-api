package uk.gov.caz.vcc.domain.service.bulkchecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;

/**
 * A utility class providing helpers to package bulk chargeability check results
 * into CSV output formatted results.
 *
 */
public class BulkCheckerUtility {

  private BulkCheckerUtility() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Copy a list.
   * 
   * @param originalList the original list
   * @return a copy of the original list
   */
  public static <T> List<T> copyList(List<T> originalList) {
    return originalList.stream().collect(Collectors.toList());
  }

  /**
   * Convert BulkComplianceCheckResponse to CsvOutput.
   * 
   * @param complianceResults a list of {@link ComplianceResultsDto}
   * @param cazs a list of {@link CleanAirZoneDto}
   * @return An instance of {@link ChargeCalculationService.CsvOutput}
   */
  public static List<CsvOutput> toCsvOutput(
      List<ComplianceResultsDto> complianceResults, List<CleanAirZoneDto> cazs) {

    // Create CSV header contents
    final List<CsvOutput> outputs = new ArrayList<>();
    CsvOutput header = new CsvOutput();
    header.setVrn("");
    header.setVehicleType("");
    header.setCharges(new ArrayListValuedHashMap<>(cazs.size() + 1));
    outputs.add(header);
    cazs.forEach(caz -> {
      int colIndex = 1;
      header.getCharges().put(++colIndex, caz.getName());
    });
    header.getCharges().put(cazs.size() + 2, "Note");
    
    // Populate vehicle charge outcomes
    List<ComplianceResultsDto> vrnSortedResults = complianceResults.stream()
        .sorted(
            Comparator.comparing(ComplianceResultsDto::getRegistrationNumber))
        .collect(Collectors.toList());
    vrnSortedResults.forEach(result -> {
      CsvOutput row = new CsvOutput();
      row.setVrn(result.getRegistrationNumber());
      String vehicleType =
          (result.getVehicleType() != null && !"null".equals(result.getVehicleType()))
              ? result.getVehicleType()
              : "-";
      row.setVehicleType(vehicleType);
      row.setCharges(getVehicleCharges(cazs, result));
      row.getCharges().put(cazs.size() + 2, result.getNote());
      outputs.add(row);
    });
    
    return outputs;
  }

  private static MultiValuedMap<Integer, String> getVehicleCharges(
      List<CleanAirZoneDto> cleanAirZones, ComplianceResultsDto result) {
    MultiValuedMap<Integer, String> vehicleChargesInColumns = 
        new ArrayListValuedHashMap<>(cleanAirZones.size());
    for (CleanAirZoneDto cleanAirZone : cleanAirZones) {
      Optional<Float> charge = result.getComplianceOutcomes().stream()
          .filter(outcome -> outcome.getCleanAirZoneId().equals(cleanAirZone.getCleanAirZoneId()))
          .map(ComplianceOutcomeDto::getCharge)
          .findFirst();
      int columnNumber = cleanAirZones.indexOf(cleanAirZone);
      if (charge.isPresent()) {
        vehicleChargesInColumns.put(columnNumber + 1, String.valueOf(charge.get()));
      } else {
        vehicleChargesInColumns.put(columnNumber + 1, "-");
      }
    }
    return vehicleChargesInColumns;
  }
}
