package uk.gov.caz.accounts.dto;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * Class that represents incoming JSON payload for Account update.
 */
@Value
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class AccountUpdateRequestDto extends AccountCommonOperationRequestDto {

}
