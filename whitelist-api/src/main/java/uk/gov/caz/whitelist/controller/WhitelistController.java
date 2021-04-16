package uk.gov.caz.whitelist.controller;

import static uk.gov.caz.whitelist.model.Actions.CREATE;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.whitelist.dto.ErrorsResponse;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDetailsResponseDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleRequestDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleResponseDto;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.service.WhitelistService;

@Slf4j
@RestController
@AllArgsConstructor
public class WhitelistController implements WhitelistControllerApiSpec {

  public static final String BASE_PATH = "/v1/whitelisting/vehicles";
  public static final String X_MODIFIER_ID_HEADER = "X-Modifier-ID";
  public static final String X_MODIFIER_EMAIL_HEADER = "X-Modifier-Email";

  private final WhitelistService whitelistService;

  @Override
  public ResponseEntity<WhitelistedVehicleDetailsResponseDto> whitelistVehicleDetails(String vrn) {
    return whitelistService.findBy(vrn)
        .map(WhitelistedVehicleDetailsResponseDto::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity addWhitelistVehicle(WhitelistedVehicleRequestDto requestDto) {
    WhitelistedVehicleDto vehicleDto = WhitelistedVehicleDto
        .from(requestDto, CREATE.getActionCharacter());
    List<ValidationError> validationErrors = vehicleDto.validate();

    if (!validationErrors.isEmpty()) {
      return ResponseEntity.badRequest().body(ErrorsResponse.from(validationErrors));
    }
    WhitelistVehicle whitelistVehicle = whitelistService
        .save(vehicleDto.mapToWhitelistVehicle(requestDto.getUploaderId()));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(WhitelistedVehicleResponseDto.from(whitelistVehicle));
  }

  @Override
  public ResponseEntity<WhitelistedVehicleResponseDto> removeWhitelistVehicle(String vrn,
      UUID modifierId, String modifierEmail) {
    return ResponseEntity.ok(
        WhitelistedVehicleResponseDto.from(whitelistService.deleteBy(vrn, modifierId,
            modifierEmail))
    );
  }
}