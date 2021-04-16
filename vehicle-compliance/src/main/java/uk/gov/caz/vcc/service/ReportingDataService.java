package uk.gov.caz.vcc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.EntrantExemption;
import uk.gov.caz.vcc.domain.EntrantTaxiPhv;
import uk.gov.caz.vcc.domain.ReportingExemptionReason;
import uk.gov.caz.vcc.domain.ReportingFuelType;
import uk.gov.caz.vcc.domain.ReportingTypeApproval;
import uk.gov.caz.vcc.domain.ReportingVehicleType;
import uk.gov.caz.vcc.domain.VehicleEntrantReporting;
import uk.gov.caz.vcc.dto.VehicleEntrantReportingRequest;
import uk.gov.caz.vcc.repository.EntrantExemptionRepository;
import uk.gov.caz.vcc.repository.EntrantTaxiPhvRepository;
import uk.gov.caz.vcc.repository.ReportingExemptionReasonRepository;
import uk.gov.caz.vcc.repository.ReportingFuelTypeRepository;
import uk.gov.caz.vcc.repository.ReportingTypeApprovalRepository;
import uk.gov.caz.vcc.repository.ReportingVehicleTypeRepository;
import uk.gov.caz.vcc.repository.VehicleEntrantReportingRepository;

/**
 * Service that take SQS messages from {@ReportingDataHandler} and saves
 * reporting data to the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingDataService {
  
  private final VehicleEntrantReportingRepository vehicleEntrantsReportingRepository;
  
  private final EntrantExemptionRepository entrantExemptionRepository;
  
  private final EntrantTaxiPhvRepository entrantTaxiPhvRepository;

  @VisibleForTesting
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
  
  private final ReportingTypeApprovalRepository reportingTypeApprovalRepository;
  
  private final ReportingFuelTypeRepository reportingFuelTypeRepository;
  
  private final ReportingVehicleTypeRepository reportingVehicleTypeRepository;
  
  private final ReportingExemptionReasonRepository reportingExemptionReasonRepository;

  /**
   * Main method that handles processing the messages from SQS to save
   * the reporting data.
   *
   * @param messageBody from SQS from {@ReportingDataHandler}.
   * @param messageId from SQS from {@ReportingDataHandler}.
   */
  public void process(String messageBody, String messageId) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      List<VehicleEntrantReportingRequest> requests = mapper.readValue(messageBody, 
          new TypeReference<List<VehicleEntrantReportingRequest>>(){});
      requests.forEach(r -> save(r)); 
    } catch (Exception ex) {
      log.error(String.format("Error while processing message with Id: %s", messageId), ex);
    }
  }  
  
  
  
  
  private void save(VehicleEntrantReportingRequest request) {
    log.info("Processing request with correlation ID {}", request.getCorrelationId());
    LocalDateTime hour = LocalDateTime.parse(request.getHour(), DATE_TIME_FORMATTER);
    VehicleEntrantReporting vehicleReport = new VehicleEntrantReporting(
        request.getCleanAirZoneId(), request.getVrnHash(), hour,
        request.getChargeValidityCode(), request.getMake(), request.getModel(),
        request.getColour(), request.isNonStandardUkPlateFormat());
    
    vehicleReport.setTypeApprovalId(getTypeApprovalId(request.getTypeApproval()));
    vehicleReport.setFuelTypeId(getFuelTypeId(request.getFuelType()));
    vehicleReport.setCcazVehicleTypeId(getCcazVehicleTypeId(request.getVehicleType()));
    
    vehicleEntrantsReportingRepository.save(vehicleReport);
    
    if (request.getChargeValidityCode().equals("CVC02")) {
      saveEntrantExemption(request, vehicleReport);
    }
    
    if (request.getTaxiPhvDescription() != null) {
      saveEntrantTaxiPhv(request, vehicleReport);
    }   
  }
  
  /**
   * Method to process and save exemption reason data.
   * 
   * @param request {@VehicleEntrantReportingRequest} 
   * @param vehicleReport {@VehicleEntrantReporting}
   */
  private void saveEntrantExemption(VehicleEntrantReportingRequest request,
      VehicleEntrantReporting vehicleReport) {
    UUID vehicleEntrantReportingId = vehicleReport.getVehicleEntrantReportingId();
    UUID exemptionReasonId = getExemptionReasonId(request.getExemptionReason());
    EntrantExemption entrantExemptionReport = new EntrantExemption(vehicleEntrantReportingId, 
        exemptionReasonId);
    entrantExemptionRepository.save(entrantExemptionReport);
  }
  
  /**
   * Method to process and save taxi entrant data.
   *
   * @param request {@VehicleEntrantReportingRequest} 
   * @param vehicleReport {@VehicleEntrantReporting}
   */
  private void saveEntrantTaxiPhv(VehicleEntrantReportingRequest request,
      VehicleEntrantReporting vehicleReport) {
    UUID vehicleEntrantReportingId = vehicleReport.getVehicleEntrantReportingId();
    List<String> licensingAuthorities = request.getLicensingAuthorities();
    String description = request.getTaxiPhvDescription();
    List<EntrantTaxiPhv> taxiEntrants = licensingAuthorities.stream().map(la -> 
        new EntrantTaxiPhv(vehicleEntrantReportingId,
        description, la)).collect(Collectors.toList());
    entrantTaxiPhvRepository.saveAll(taxiEntrants);
  }
  
  /**
   * Method to map typeApproval to typeApprovalId.
   *
   * @param typeApproval as a String.
   */
  @VisibleForTesting 
  public UUID getTypeApprovalId(String typeApproval) {
    ReportingTypeApproval reportingTypeApproval = 
        reportingTypeApprovalRepository.findTypeApprovalId(typeApproval);
    
    return reportingTypeApproval.getTypeApprovalId();
  }
  
  /**
   * Method to map fuelType to fuelTypeId.
   *
   * @param fuelType as a String.
   */
  @VisibleForTesting
  public UUID getFuelTypeId(String fuelType) {
    ReportingFuelType reportingFuelType = 
        reportingFuelTypeRepository.findFuelTypeId(fuelType);
    
    return reportingFuelType.getFuelTypeId();
  }
  
  /**
   * Method to map {@VehicleType} to ccazVehicleTypeId.
   *
   * @param vehicleType as {@VehicleType}
   */
  @VisibleForTesting
  public UUID getCcazVehicleTypeId(VehicleType vehicleType) {
    ReportingVehicleType reportingVehicleType = 
        reportingVehicleTypeRepository.findVehicleTypeId(vehicleType);
    
    return reportingVehicleType.getVehicleTypeId();
  }
  
  /**
   * Method to map exemptionReason to exemptionReasonId.
   *
   * @param exemptionReason as a String.
   */
  @VisibleForTesting
  public UUID getExemptionReasonId(String exemptionReason) {
    ReportingExemptionReason reportingExemptionReason = 
        reportingExemptionReasonRepository.findExemptionReasonId(exemptionReason);
    
    return reportingExemptionReason.getExemptionReasonId();
  }

}
