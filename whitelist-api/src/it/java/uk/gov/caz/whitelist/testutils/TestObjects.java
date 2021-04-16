package uk.gov.caz.whitelist.testutils;

import java.util.Optional;
import java.util.UUID;
import uk.gov.caz.whitelist.model.WhitelistVehicle;

public class TestObjects {

  public static WhitelistVehicle createRandomWhitelistVehicle(String vrn) {
    return createWhitelistVehicle(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        UUID.randomUUID(), vrn, "Other", "test@gov.uk");
  }

  public static WhitelistVehicle createWhitelistVehicle(String reasonUpdated, String manufacturer,
      UUID uploaderId, String vrn, String category, String email) {
    return WhitelistVehicle.builder()
        .vrn(vrn)
        .category(category)
        .reasonUpdated(reasonUpdated)
        .manufacturer(manufacturer)
        .uploaderId(uploaderId)
        .uploaderEmail(email)
        .build();
  }

  public static Optional<WhitelistVehicle> whitelistedVehicle() {
    return Optional.of(WhitelistVehicle.builder()
        .vrn("CAS310")
        .category("Other")
        .manufacturer("manufacturer")
        .reasonUpdated("reasonUpdated")
        .uploaderEmail("test@gov.uk")
        .uploaderId(UUID.randomUUID())
        .build());
  }
}