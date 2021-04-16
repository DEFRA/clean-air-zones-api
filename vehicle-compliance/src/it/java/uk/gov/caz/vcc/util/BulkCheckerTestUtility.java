package uk.gov.caz.vcc.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.vcc.repository.LocalVehicleDetailsRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;

@Component
public class BulkCheckerTestUtility {

  @Autowired
  RetrofitRepository retrofitRepository;

  @Autowired
  GeneralWhitelistRepository gwlRepository;

  @Autowired(required = false)
  LocalVehicleDetailsRepository vehicleRepository;

  // creation methods

  public CleanAirZoneDto createCleanAirZoneDto(UUID cleanAirZoneId, String cleanAirZoneName) {
    return CleanAirZoneDto.builder()
        .cleanAirZoneId(cleanAirZoneId)
        .name(cleanAirZoneName)
        .build();
  }
  
  public void createRetrofitVehicle(String vrn) {
    RetrofittedVehicle retrofittedVehicle = new RetrofittedVehicle();
    retrofittedVehicle.setVrn(vrn);
    retrofittedVehicle.setDateOfRetrofit(new java.sql.Date(2016, 3, 1));
    retrofitRepository.save(retrofittedVehicle);
  }
  
  public void createMultipleRetrofitVehicles(List<String> vrns) {
    for (String vrn : vrns) {
      createRetrofitVehicle(vrn);
    }
  }

  public void createGeneralWhiteListVehicle(String vrn, boolean exempt, boolean compliant) {
    GeneralWhitelistVehicle gwlVehicle = new GeneralWhitelistVehicle();
    gwlVehicle.setVrn(vrn);
    gwlVehicle.setExempt(exempt);
    gwlVehicle.setCompliant(compliant);
    gwlVehicle.setReasonUpdated("reasonUpdated");
    gwlVehicle.setCategory("category");
    gwlVehicle.setUploaderId(UUID.fromString("b103c632-c8b6-496c-aaeb-31c3ef10a489"));
    gwlVehicle.setUpdateTimestamp(LocalDateTime.now());
    gwlRepository.save(gwlVehicle);
  }  
  
  // deletion methods

  public void deleteRetrofitVehicle(String vrn) {
    RetrofittedVehicle retrofittedVehicle = retrofitRepository.findByVrnIgnoreCase(vrn);
    retrofitRepository.delete(retrofittedVehicle);
  }

  public void deleteMultipleRetrofitVehicles(List<String> vrns) {
    vrns.forEach(vrn -> {
      deleteRetrofitVehicle(vrn);
    });
  }

  public void deleteGeneralWhiteListVehicle(String vrn) {
    Optional<GeneralWhitelistVehicle> gwlVehicle = gwlRepository.findByVrnIgnoreCase(vrn);
    gwlVehicle.ifPresent(generalWhitelistVehicle -> gwlRepository.delete(generalWhitelistVehicle));
  }

  public void deleteGeneralWhiteListVehicles(List<String> vrns) {
    vrns.stream()
        .forEach(vrn -> {
          deleteGeneralWhiteListVehicle(vrn);
        });
  }
}