package uk.gov.caz.vcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.vcc.service.BulkCheckerService.BulkCheckerCsvFile;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;

@ExtendWith(MockitoExtension.class)
public class BulkCheckerServiceTest {

  @InjectMocks
  BulkCheckerService bulkCheckerService;
  
  @Mock
  S3Client mockedS3Client;

  @Mock
  ChargeCalculationService mockedChargeCalculationService;
  
  @Mock
  CazTariffService tariffService;

  @Test
  public void shouldProcessS3File() throws InterruptedException {
    String BUCKET = "S3Bucket";
    String FILENAME = "input/input.csv";
    String CSV_CONTENT = "CAS310\r\nCAS312";
    // given
    mockS3Service(CSV_CONTENT);
    List<CleanAirZoneDto> cleanAirZones = mockTariffService();
    mockCsvOutputs(cleanAirZones, CSV_CONTENT);
    bulkCheckerService.process(BUCKET, FILENAME, 60);
    // then
    ArgumentCaptor<List<String>> vrnListArg = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<CleanAirZoneDto>> cazListArg = ArgumentCaptor.forClass(List.class);
    verify(mockedChargeCalculationService, times(1)).getComplianceCheckAsCsv(
        vrnListArg.capture(), cazListArg.capture());
    assertEquals(2, vrnListArg.getValue().size());
    ArgumentCaptor<PutObjectRequest> putRequestArg = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(mockedS3Client, times(1)).putObject(putRequestArg.capture(), any(RequestBody.class));
    assertEquals("output/input-output.csv", putRequestArg.getValue().key());
  }

  private List<CleanAirZoneDto> mockTariffService() {
    List<CleanAirZoneDto> cleanAirZones = new ArrayList<CleanAirZoneDto>();
    cleanAirZones.add(mockCleanAirZoneDto("Test One"));
    cleanAirZones.add(mockCleanAirZoneDto("Test Two"));
    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(cleanAirZones)
        .build();
    
    Mockito.when(tariffService.getCleanAirZoneSelectionListings()).thenReturn(cleanAirZonesDto);
    return cleanAirZones;
  }

  private void mockCsvOutputs(List<CleanAirZoneDto> cleanAirZones, String content) 
      throws InterruptedException {
    List<CsvOutput> csvOutputs = new ArrayList<>();
    String[] vrns = content.split("\r\n");

    csvOutputs.add(mockOutput("", "", "Birmingham", "Bath"));
    for (int i = 0; i < vrns.length; i++) {
      csvOutputs.add(mockOutput(vrns[i], "CAR", "0", "0"));
    }
    when(mockedChargeCalculationService.getComplianceCheckAsCsv(
        Stream.of(vrns).collect(Collectors.toList()), cleanAirZones)).thenReturn(csvOutputs);
  }
  
  private CleanAirZoneDto mockCleanAirZoneDto(String cleanAirZoneName) {
    return CleanAirZoneDto.builder()
        .name(cleanAirZoneName)
        .build();
  }

  private CsvOutput mockOutput(String vrn, String vehicleType, String birminghamCharge, String leedCharge) {
    CsvOutput output = new CsvOutput();
    MultiValuedMap<Integer, String> charges = new ArrayListValuedHashMap<>();
    output.setVrn(vrn);
    output.setVehicleType(vehicleType);
    output.setCharges(charges);
    charges.put(0, birminghamCharge);
    charges.put(1, leedCharge);
    return output;
  }

  private void mockS3Service(String csvContent) {
    InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
    ResponseBytes<GetObjectResponse> mockResponseBytes = Mockito.mock(ResponseBytes.class);
    when(mockResponseBytes.asInputStream()).thenReturn(inputStream);
    when(mockedS3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(mockResponseBytes);
  }

  @Test
  public void shouldReturnOutputFile() {
    when(mockedS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
    String fileName = "input.csv";
    bulkCheckerService.setFilePrefix("");
    bulkCheckerService.setS3Bucket("jaqu.caz");
    try {
      BulkCheckerCsvFile output = bulkCheckerService.getBulkCheckerOutputFile(fileName);
      assertEquals("output/input-output.csv", output.getFileName());
      assertEquals("jaqu.caz", output.getS3Bucket());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void shouldNotReturnOutputFile() {
    when(mockedS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.class).thenReturn(null);
    String fileName = "input.csv";
    bulkCheckerService.setFilePrefix("");
    bulkCheckerService.setS3Bucket("jaqu.caz");
    try {
      BulkCheckerCsvFile output = bulkCheckerService.getBulkCheckerOutputFile(fileName);
      assertNull(output.getFileName());
      assertEquals("jaqu.caz", output.getS3Bucket());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void shouldThrowFileNotFoundException() {
    when(mockedS3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(NoSuchKeyException.class)
        .thenThrow(NoSuchKeyException.class);
    String fileName = "input.csv";
    bulkCheckerService.setFilePrefix("");
    bulkCheckerService.setS3Bucket("jaqu.caz");
    assertThrows(FileNotFoundException.class, () -> bulkCheckerService.getBulkCheckerOutputFile(fileName));
  }
}