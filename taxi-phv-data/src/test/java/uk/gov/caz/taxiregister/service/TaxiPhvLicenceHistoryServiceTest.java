package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalRequest;
import uk.gov.caz.taxiregister.model.LicenceInfoHistorical;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicenceHistory;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceHistoryPostgresRepository;

@ExtendWith(MockitoExtension.class)
class TaxiPhvLicenceHistoryServiceTest {

  @Mock
  private TaxiPhvLicenceHistoryPostgresRepository repository;

  @InjectMocks
  private TaxiPhvLicenceHistoryService service;

  @Test
  void shouldReturnPageAndCallDatabaseForCountIfPageSizeEqualReturnedListSize() {
    //given
    int pageSize = 10;
    long totalCount = 20;

    LicenceInfoHistoricalRequest request = create(pageSize, 0);
    mockFindByVrmInRange(request, getListWithSize(pageSize));

    mockCount(totalCount, request);

    //when
    LicenceInfoHistorical historicalDto = service.findByVrmInRange(request);

    //then
    assertThat(historicalDto.getTotalChangesCount()).isEqualTo(totalCount);
    assertThat(historicalDto.getChanges()).hasSize(pageSize);
    verify(repository, times(1)).count(anyString(), any(), any());
  }

  @Test
  void shouldReturnPageAndCallDatabaseForCountIfPageNumberOtherThenZero() {
    //given
    int pageSize = 10;
    long totalCount = 20;
    int pageNumber = 1;
    LicenceInfoHistoricalRequest request = create(pageSize, pageNumber);

    mockFindByVrmInRange(request, getListWithSize(pageSize));
    mockCount(totalCount, request);

    //when
    LicenceInfoHistorical historicalDto = service.findByVrmInRange(request);

    //then
    assertThat(historicalDto.getTotalChangesCount()).isEqualTo(totalCount);
    assertThat(historicalDto.getChanges()).hasSize(pageSize);
    verify(repository, times(1)).count(anyString(), any(), any());
  }

  @Test
  void shouldNotCallDBForCountIfReturnedListSizeSmallerThanPageSize() {
    //given
    int pageSize = 10;
    long totalCount = pageSize - 1;
    int pageNumber = 0;
    LicenceInfoHistoricalRequest request = create(pageSize, pageNumber);

    mockFindByVrmInRange(request, getListWithSize(totalCount));

    //when
    LicenceInfoHistorical historicalDto = service.findByVrmInRange(request);

    //then
    assertThat(historicalDto.getTotalChangesCount()).isEqualTo(totalCount);
    assertThat(historicalDto.getChanges()).hasSize((int) totalCount);
    verify(repository, never()).count(anyString(), any(), any());
  }

  private void mockCount(long totalCount, LicenceInfoHistoricalRequest request) {
    when(repository.count(request.getVrm(), request.getModifyDateFrom(), request.getModifyDateTo()))
        .thenReturn(totalCount);
  }

  private LicenceInfoHistoricalRequest create(long pageSize, int pageNumber) {
    return LicenceInfoHistoricalRequest.builder()
        .pageNumber(pageNumber)
        .pageSize(pageSize)
        .vrm("vrm")
        .modifyDateFrom(LocalDateTime.MIN)
        .modifyDateTo(LocalDateTime.MAX)
        .build();
  }

  private void mockFindByVrmInRange(LicenceInfoHistoricalRequest request,
      List<TaxiPhvVehicleLicenceHistory> listWithSize) {
    when(repository
        .findByVrmInRange(request.getVrm(), request.getModifyDateFrom(), request.getModifyDateTo(),
            request.getPageSize(), request.getPageNumber())).thenReturn(listWithSize);
  }

  public List<TaxiPhvVehicleLicenceHistory> getListWithSize(long size) {
    return LongStream.range(0, size)
        .mapToObj(i -> TaxiPhvVehicleLicenceHistory.builder().build())
        .collect(Collectors.toList());
  }
}