package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class NoOpPaymentsServiceTest {

  private NoOpPaymentsService paymentsService = new NoOpPaymentsService();

  @Test
  public void shouldReturnNotPaidStatusAndNullPaymentMethod() {
    // given
    EntrantPaymentRequestDto anyRequest = EntrantPaymentRequestDto.builder().build();

    // when
    EntrantPaymentDtoV1 result = paymentsService.registerVehicleEntryAndGetPaymentStatus(anyRequest);

    // then
    assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_PAID);
    assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.NULL);
    assertThat(result.getEntrantPaymentId()).isNull();
  }

}