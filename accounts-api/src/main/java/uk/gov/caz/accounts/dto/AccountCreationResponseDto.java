package uk.gov.caz.accounts.dto;

import lombok.Value;

/**
 * Class that represents the JSON structure for response for Account creation.
 */
@Value(staticConstructor = "of")
public class AccountCreationResponseDto {

  String accountId;
}