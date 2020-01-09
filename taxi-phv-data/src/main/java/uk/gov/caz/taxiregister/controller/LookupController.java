package uk.gov.caz.taxiregister.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.dto.LicenceInfo;
import uk.gov.caz.taxiregister.service.LookupService;

@RestController
public class LookupController implements LookupControllerApiSpec {

  public static final String PATH = "/v1/vehicles/{vrn}/licence-info";

  private final LookupService licenceLookupService;

  /**
   * Creates an instance of {@link LookupController}.
   *
   * @param licenceLookupService An instance of {@link LookupService}.
   */
  public LookupController(LookupService licenceLookupService) {
    this.licenceLookupService = licenceLookupService;
  }

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
}
