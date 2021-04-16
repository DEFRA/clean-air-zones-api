package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.dto.ModVehicleDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesRequestDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesResponseDto;

@ExtendWith(MockitoExtension.class)
class HttpBasedModRepositoryTest {

  @Mock
  private RetrofitModRepository retrofitModRepository;
  @Mock
  private AsyncRestService asyncRestService;

  @InjectMocks
  private HttpBasedModRepository repository;

  @Nested
  class ExistByVrns {

    @Nested
    class WhenListExceedsMaxBatchSize {

      @Test
      public void shouldProcessInBatches() {
        // given
        Set<String> vrns = generateVrns();
        mockPresenceOfOddVrnInResponse();

        // when
        Map<String, Boolean> result = repository.existByVrns(vrns);

        // then
        assertThat(result).hasSameSizeAs(vrns);
        verify(retrofitModRepository, times(2)).findModVehiclesAsync(any());
      }

      private void mockPresenceOfOddVrnInResponse() {
        doAnswer(answer -> {
          GetModVehiclesRequestDto argument = answer.getArgument(0);
          Map<String, ModVehicleDto> result = argument.getVrns()
              .stream()
              .filter(vrn -> vrn.charAt(vrn.length() - 1) % 2 == 1)
              .collect(
                  Collectors.toMap(Function.identity(),
                      vrn -> ModVehicleDto.builder().vrn(vrn).whitelistDiscountCode("WDC001").build()
                  )
              );
          return  AsyncOp.asCompletedAndSuccessful(
              String.valueOf(argument.hashCode()),
              HttpStatus.OK,
              new GetModVehiclesResponseDto(result)
          );
        }).when(retrofitModRepository).findModVehiclesAsync(any());
      }

      private Set<String> generateVrns() {
        return IntStream
            .rangeClosed(1, HttpBasedModRepository.EXIST_BY_VRNS_MAX_BATCH_SIZE + 1)
            .mapToObj(i -> "A1" + i)
            .collect(Collectors.toSet());
      }
    }

  }
}