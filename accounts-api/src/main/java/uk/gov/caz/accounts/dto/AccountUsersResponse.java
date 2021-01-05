package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for User for Account retrieval response.
 */
@Value
@Builder
public class AccountUsersResponse {

  /**
   * Information whether this account is multi-payer.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.account-users.multi-payer-account}")
  boolean multiPayerAccount;

  /**
   * The list of VRNs associated with the account ID provided in the request.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.account-users.users}")
  List<AccountUserResponse> users;
}
