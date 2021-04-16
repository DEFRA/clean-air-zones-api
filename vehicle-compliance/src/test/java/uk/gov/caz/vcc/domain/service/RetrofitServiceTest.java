package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.service.RetrofitService;
import uk.gov.caz.vcc.service.RetrofitService.RetrofitQueryResponse;

@ExtendWith(MockitoExtension.class)
public class RetrofitServiceTest {
  @Mock
  RetrofitRepository retrofitRepository;

  List<String> vrns = Arrays.asList("InvalidSQL");
  
  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(retrofitRepository.findRetrofitVehicleByVrns(vrns)).thenThrow(RuntimeException.class);
  }
}