package uk.gov.caz.accounts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.util.Date;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;

/**
 * Class that represents the JSON structure for response when creating DirectDebitMandate.
 */
@Value
@Builder
public class DirectDebitMandateDto {

  /**
   * Primary key for T_ACCOUNT_DIRECT_DEBIT_MANDATE table.
   */
  UUID directDebitMandateId;

  /**
   * An identifier of the associated Account.
   */
  UUID accountId;

  /**
   * An identifier of the associated AccountUser.
   */
  UUID accountUserId;

  /**
   * An identifier of the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * Identifier generated for the mandate by GOV.UK Pay
   */
  String paymentProviderMandateId;

  /**
   * Status of the direct debit mandate.
   */
  DirectDebitMandateStatus status;

  /**
   * Date and time the mandate was created.
   */
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  Date created;
  
  /**
   * Transforms provided {@link DirectDebitMandate} to {@link DirectDebitMandateDto} object.
   *
   * @param directDebitMandate provided directDebit object
   * @return {@link DirectDebitMandateDto}
   */
  public static DirectDebitMandateDto from(DirectDebitMandate directDebitMandate) {
    return DirectDebitMandateDto
        .builder()
        .directDebitMandateId(directDebitMandate.getId())
        .accountId(directDebitMandate.getAccountId())
        .accountUserId(directDebitMandate.getAccountUserId())
        .cleanAirZoneId(directDebitMandate.getCleanAirZoneId())
        .paymentProviderMandateId(directDebitMandate.getPaymentProviderMandateId())
        .status(directDebitMandate.getStatus())
        .created(directDebitMandate.getCreated())
        .build();
  }
}
