package uk.gov.caz.vcc.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.vcc.repository.RetrofitRepository;

@ExtendWith(MockitoExtension.class)
class RetrofitServiceTest {
  @Mock
  RetrofitRepository retrofitRepository;

  @InjectMocks
  RetrofitService retrofitService;
  
  @Test
  void findByVrnTest() {
    retrofitService.findByVrn("vrn");
    verify(retrofitRepository).findByVrnIgnoreCase("vrn");
    verifyNoMoreInteractions(retrofitRepository);
  }

  @Test
  void isRetrofittedTest() {
    retrofitService.isRetrofitted("vrn");
    verify(retrofitRepository).existsByVrnIgnoreCase("vrn");
    verifyNoMoreInteractions(retrofitRepository);
  }
}