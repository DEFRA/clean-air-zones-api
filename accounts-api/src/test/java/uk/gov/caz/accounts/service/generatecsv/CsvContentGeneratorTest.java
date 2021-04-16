package uk.gov.caz.accounts.service.generatecsv;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.VccsRepository;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;

@ExtendWith(MockitoExtension.class)
class CsvContentGeneratorTest {

  private final static UUID CAZ_1_ID = UUID.randomUUID();
  private final static UUID CAZ_2_ID = UUID.randomUUID();
  private final static UUID CAZ_3_ID = UUID.randomUUID();

  @Mock
  private VccsRepository vccsRepository;

  @Mock
  private AccountVehicleRepository accountVehicleRepository;

  private CsvContentGenerator csvGeneratorService;

  @BeforeEach
  public void setup() {
    csvGeneratorService = new CsvContentGenerator(vccsRepository, accountVehicleRepository);
    mockClearAirZones();
  }

  @Test
  public void shouldGenerateHeaderRow() {

    String headers = csvGeneratorService.generateHeaders();

    assertThat(headers).isNotNull();
    assertThat(headers)
        .isEqualTo("Number plate,Vehicle Type,Bath (Live),Birmingham (Upcoming),Test (Upcoming)");
  }

  private void mockClearAirZones() {
    CleanAirZoneDto caz1 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_1_ID)
        .activeChargeStartDate("2021-08-20")
        .name("Birmingham")
        .build();
    CleanAirZoneDto caz2 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_2_ID)
        .activeChargeStartDate("2023-08-20")
        .name("Test")
        .build();
    CleanAirZoneDto caz3 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_3_ID)
        .activeChargeStartDate("2019-08-20")
        .name("Bath")
        .build();
    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(newArrayList(caz1, caz2, caz3))
        .build();
    Response<CleanAirZonesDto> cazResponse = Response.success(cleanAirZonesDto);
    lenient().when(vccsRepository.findCleanAirZonesSync()).thenReturn(cazResponse);
  }
}