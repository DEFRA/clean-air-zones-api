package uk.gov.caz.accounts.dto;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterCsvFromS3LambdaInput {

  int registerJobId;
  String s3Bucket;
  String fileName;
  String correlationId;
  String action;
  Boolean shouldSendEmailsUponSuccessfulChargeCalculation;

  /**
   * Getter for {@code shouldSendEmailsUponSuccessfulChargeCalculation} that returns either the
   * provided value (if not null) or the default one (true).
   */
  public boolean shouldSendEmailsUponSuccessfulChargeCalculation() {
    return Objects.isNull(shouldSendEmailsUponSuccessfulChargeCalculation)
        || shouldSendEmailsUponSuccessfulChargeCalculation;
  }
}
