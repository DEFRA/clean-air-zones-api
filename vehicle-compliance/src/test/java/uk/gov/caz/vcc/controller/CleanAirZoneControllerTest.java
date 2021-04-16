package uk.gov.caz.vcc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.vcc.service.CazTariffService;

@ExtendWith(MockitoExtension.class)
class CleanAirZoneControllerTest {

  @Mock
  private CazTariffService cazTariffService;

  @InjectMocks
  private CleanAirZoneController cleanAirZoneController;

  @Test
  void shouldReturnCazDetailsAndStatusOK() throws URISyntaxException {

    CleanAirZoneDto testCleanAirZone = CleanAirZoneDto.builder()
        .boundaryUrl(new URI("http://test")).name("Test")
        .activeChargeStartDate("2020-10-28")
        .operatorName("Operator Name")
        .mainInfoUrl(URI.create("mainInfoUrl.gov.uk"))
        .cleanAirZoneId(UUID.randomUUID()).build();

    List<CleanAirZoneDto> testListWrapper = new ArrayList<>();

    testListWrapper.add(testCleanAirZone);

    CleanAirZonesDto cleanAirZonesWrapper = CleanAirZonesDto.builder()
        .cleanAirZones(testListWrapper).build();

    given(cazTariffService.getCleanAirZoneSelectionListings())
        .willReturn(cleanAirZonesWrapper);

    // when
    ResponseEntity<CleanAirZonesDto> response = cleanAirZoneController
        .getCleanAirZones();

    assertThat(response).isNotNull();
    assertThat(response.getBody().getCleanAirZones().size())
        .isEqualTo(testListWrapper.size());
    assertThat(response.getBody().getCleanAirZones().get(0).getCleanAirZoneId())
        .isEqualTo(testCleanAirZone.getCleanAirZoneId());
    assertThat(response.getBody().getCleanAirZones().get(0).getActiveChargeStartDate())
        .isEqualTo("2020-10-28");
    assertThat(response.getBody().getCleanAirZones().get(0).getOperatorName())
        .isEqualTo(testCleanAirZone.getOperatorName());
    assertThat(response.getBody().getCleanAirZones().get(0).getMainInfoUrl())
        .isEqualTo(testCleanAirZone.getMainInfoUrl());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
