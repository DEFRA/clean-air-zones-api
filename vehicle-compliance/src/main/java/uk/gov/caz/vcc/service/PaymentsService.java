package uk.gov.caz.vcc.service;

import java.util.List;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;

public interface PaymentsService {

  /**
   * Get payment status from payments service. Supports 'old' API.
   *
   * @param request {@link EntrantPaymentRequestDto}.
   * @return {@link EntrantPaymentDtoV1}.
   */
  EntrantPaymentDtoV1 registerVehicleEntryAndGetPaymentStatus(EntrantPaymentRequestDto request);

  /**
   * Get payment status from payments service. Supports API that returns EntrantPaymentDtoV2.
   *
   * @param requests a list of {@link EntrantPaymentRequestDto}.
   * @return {@link EntrantPaymentDtoV1}.
   */
  List<EntrantPaymentDtoV2> registerVehicleEntriesAndGetPaymentStatus(
      List<EntrantPaymentRequestDto> requests);
}
