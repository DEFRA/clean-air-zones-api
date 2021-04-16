package uk.gov.caz.taxiregister.controller;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.dto.LicenceInfo;
import uk.gov.caz.taxiregister.dto.lookup.GetLicencesInfoRequestDto;
import uk.gov.caz.taxiregister.dto.lookup.GetLicencesInfoResponseDto;
import uk.gov.caz.taxiregister.service.LookupService;

@RestController
@RequiredArgsConstructor
public class LookupController implements LookupControllerApiSpec {

  public static final String PATH = "/v1/vehicles/{vrn}/licence-info";
  public static final String BULK_PATH = "/v1/vehicles/licences-info/search";

  private final LookupService licenceLookupService;

  @Override
  public ResponseEntity<LicenceInfo> getLicenceInfoFor(@PathVariable String vrn) {
    return licenceLookupService.getLicenceInfoBy(vrn)
        .map(LicenceInfo::from)
        .map(licenceInfo -> ResponseEntity.ok()
            .body(licenceInfo))
        .orElseGet(() -> ResponseEntity.notFound()
            .build()
        );
  }

  @Override
  public ResponseEntity<GetLicencesInfoResponseDto> getLicencesInfoFor(
      GetLicencesInfoRequestDto request) {
    request.validate();

    Map<String, LicenceInfo> result = licenceLookupService.getLicencesInfoFor(request.getVrms())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, e -> LicenceInfo.from(e.getValue())));
    return ResponseEntity.ok(GetLicencesInfoResponseDto.from(result));
  }
}
