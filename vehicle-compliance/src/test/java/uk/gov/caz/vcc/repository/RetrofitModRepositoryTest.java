package uk.gov.caz.vcc.repository;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.vcc.dto.ModVehicleDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesRequestDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesResponseDto;

@ExtendWith(MockitoExtension.class)
class RetrofitModRepositoryTest {

  @Test
  public void shouldConstructProperAsyncOp() {
    //given
    String correlationId = UUID.randomUUID().toString();
    String vrn = UUID.randomUUID().toString();
    ModVehicleDto modVehicleDto = ModVehicleDto.builder().vrn(vrn).build();
    RetrofitModRepository retrofitModRepository = new TestImplementation(mock(Call.class));

    //when
    AsyncOp<ModVehicleDto> asyncOp = retrofitModRepository
        .findByRegistrationNumberAsync(correlationId, modVehicleDto.getVrn());

    //then
    assertThat(asyncOp.getIdentifier()).isEqualTo("MOD: " + correlationId);
  }

  @RequiredArgsConstructor
  class TestImplementation implements RetrofitModRepository {

    private final Call<ModVehicleDto> call;

    @Override
    public Call<ModVehicleDto> findModVehicle(String correlationId, String vrn) {
      return call;
    }

    @Override
    public Call<GetModVehiclesResponseDto> findModVehicles(GetModVehiclesRequestDto request) {
      return null;
    }
  }
}