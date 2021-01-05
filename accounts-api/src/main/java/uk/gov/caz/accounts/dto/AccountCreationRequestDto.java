package uk.gov.caz.accounts.dto;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * Class that represents incoming JSON payload for Account creation.
 */
@Value
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class AccountCreationRequestDto extends AccountCommonOperationRequestDto {

}
