package uk.gov.caz.vcc.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.exceptions.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.domain.service.compliance.BathComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusNullComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusPresentComplianceService;
import uk.gov.caz.vcc.dto.PreFetchedDataResults;
import uk.gov.caz.vcc.service.GeneralWhitelistService;
import uk.gov.caz.vcc.service.RetrofitService;


/**
 * Domain service to determine whether or not a Vehicle is compliant.
 * 
 */
@Component
@RequiredArgsConstructor
public class ComplianceService {
  private final BathComplianceService bathComplianceService;
  private final EuroStatusNullComplianceService euroStatusNullComplianceService;
  private final EuroStatusPresentComplianceService euroStatusPresentComplianceService;
  private final GeneralWhitelistService generalWhitelistService;
  private final RetrofitService retrofitService;
  
  @Value("${application.bath-caz-identifier}")
  private String bathCazIdentifier;

  /**
   * Method to determine if a Vehicle is compliant in a given CAZ.
   * 
   * @param vehicle Vehicle whose compliance is to be determined.
   * @return boolean {@code true} if compliant, else {@code false}.
   */
  public boolean isVehicleCompliant(Vehicle vehicle,
      CalculationResult calculationResult) {
    Supplier<Boolean> whitelistFlagSupplier = new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        return generalWhitelistService
            .compliantOnGeneralWhitelist(vehicle.getRegistrationNumber());
      }
    };

    Supplier<Boolean> retrofitFlagSupplier = new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        return retrofitService.isRetrofitted(vehicle.getRegistrationNumber());
      }
    };

    return this.isVehicleCompliant(vehicle, calculationResult,
        whitelistFlagSupplier, retrofitFlagSupplier);
  }

  /**
   * Method (overload) to determine if a Vehicle is compliant in a given CAZ.
   * 
   * @param vehicle Vehicle whose compliance is to be determined.
   * @param cazId UUID of the CAZ for which compliance is being determined.
   * @param isCompliantOnGeneralWhitelist a flag that can be used in the event
   *        compliant status due to existing on the general Whitelist is known
   *        at the point of invocation.
   * @param isRetrofit a flag that can be used in the event Retrofit status is
   *        known at the point of invocation.
   * @return boolean {@code true} if compliant, else {@code false}.
   */
  private boolean isVehicleCompliant(Vehicle vehicle,
      CalculationResult calculationResult,
      boolean isCompliantOnGeneralWhitelist, boolean isRetrofit) {
    return this.isVehicleCompliant(vehicle, calculationResult,
        () -> isCompliantOnGeneralWhitelist, () -> isRetrofit);
  }
  
  /**
   * Method to determine if a Vehicle is compliant in a given CAZ.
   * 
   * @param vehicle Vehicle whose compliance is to be determined.
   * @param cazId UUID of the CAZ for which compliance is being determined.
   * @param isCompliantOnGeneralWhitelistFlagSupplier a method signature for
   *        retrieving whether or not a vehicle is deemed compliant on the
   *        General Whitelist.
   * @param isRetrofit a method signature for retrieving whether or not a
   *        vehicle is exists on the Retrofit register.
   * @return boolean {@code true} if compliant, else {@code false}.
   */
  private boolean isVehicleCompliant(Vehicle vehicle,
      CalculationResult calculationResult,
      Supplier<Boolean> isCompliantOnGeneralWhitelistFlagSupplier,
      Supplier<Boolean> isRetrofitFlagSupplier) {

    UUID cazId = calculationResult.getCazIdentifier();
    calculationResult.setIsRetrofitted(false);
   
    if (cazId.toString().equals(bathCazIdentifier)
        && bathComplianceService.isVehicleCompliant(vehicle).isPresent()) {
      return true;
    }

    if (isCompliantOnGeneralWhitelistFlagSupplier.get()) {
      return true;
    }

    if (isRetrofitFlagSupplier.get()) {
      calculationResult.setIsRetrofitted(true);
      return true;
    }

    if (vehicle.getEuroStatus() == null) {
      return euroStatusNullComplianceService.isVehicleCompliant(vehicle);
    } else {
      return euroStatusPresentComplianceService.isVehicleCompliant(vehicle);
    }
  }
  
  /**
   * ArgumentOutOfRangeException Set whether the charge calculation is compliant.
   * 
   * @param vehicle A vehicle.
   * @param result  A charge calculation result.
   * @param vrnIsCompliantOnGeneralComplianceWhitelist a flag that can be used in the event
   *        compliant status due to existing on the general Whitelist is known
   *        at the point of invocation.
   * @param vehicleIsRetrofitted a flag that can be used in the event Retrofit status is
   *        known at the point of invocation.
   * @return The charge calculation object.
   */
  public CalculationResult updateCalculationResult(Vehicle vehicle,
      CalculationResult result,
      boolean vrnIsCompliantOnGeneralComplianceWhitelist,
      boolean vehicleIsRetrofitted) {
    result.setCompliant(this.isVehicleCompliant(vehicle, result,
        vrnIsCompliantOnGeneralComplianceWhitelist, vehicleIsRetrofitted));
    return result;
  }
  
  /**
   * ArgumentOutOfRangeException Set whether the charge calculation is compliant.
   * 
   * @param vehicle A vehicle.
   * @param result  A charge calculation result.
   * @return The charge calculation object.
   */
  public CalculationResult updateCalculationResult(Vehicle vehicle, CalculationResult result) {
    result.setCompliant(this.isVehicleCompliant(vehicle, result));
    return result;
  }
  
  /**
   * Overload for bulk compliance check on list of vehicles that accepts
   * pre-fetched data.
   */
  public Map<String, List<CalculationResult>> bulkComplianceCheck(
      List<Vehicle> vehicles, List<UUID> cazs,
      PreFetchedDataResults preFetchedDataResults) {
    Map<String, List<CalculationResult>> complianceCheckOutcomes =
        new HashMap<>();

    for (Vehicle vehicle : vehicles) {
      List<CalculationResult> results = new ArrayList<>();
      boolean gpwCompliant = generalWhitelistService
          .isCompliantOnGeneralPurposeWhitelist(vehicle.getRegistrationNumber(),
              preFetchedDataResults.getMatchedGeneralWhitelistVehicles());
      boolean retroFit = retrofitService.isRetrofitVehicle(
          vehicle.getRegistrationNumber(),
          preFetchedDataResults.getMatchedRetrofittedVehicles());

      for (UUID cazId : cazs) {
        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setCazIdentifier(cazId);
        try {
          calculationResult.setCompliant(isVehicleCompliant(vehicle,
              calculationResult, gpwCompliant, retroFit));
        } catch (UnableToIdentifyVehicleComplianceException ex) {
          calculationResult.unableToIdentifyVehicleCompliant();
        }
        results.add(calculationResult);
      }
      complianceCheckOutcomes.put(vehicle.getRegistrationNumber(), results);
    }

    return complianceCheckOutcomes;
  }
}
