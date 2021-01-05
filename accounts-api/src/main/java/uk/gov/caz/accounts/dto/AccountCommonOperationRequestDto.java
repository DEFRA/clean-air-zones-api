package uk.gov.caz.accounts.dto;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents incoming JSON payload for Account creation or update.
 */
@SuperBuilder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class AccountCommonOperationRequestDto {

  /**
   * Name of the account. It is not unique.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.account.account-name}")
  @Size(min = 1, max = 180)
  String accountName;

  private static final Map<Function<AccountCommonOperationRequestDto, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<AccountCommonOperationRequestDto, Boolean>, String>
          builder()
          .put(accountsRequestDto -> accountsRequestDto.accountName != null,
              "Account name cannot be null.")
          .put(accountsRequestDto -> isNotBlank(accountsRequestDto.accountName),
              "Account name cannot be empty.")
          .put(accountsRequestDto -> accountsRequestDto.accountName.length() <= 180,
              "Account name is too long.")
          .put(accountsRequestDto -> accountsRequestDto
                  .accountName.matches("[\\p{L}0-9 &/\\-'.!,]+"),
              "Account name cannot include invalid characters.")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public AccountCommonOperationRequestDto validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });

    return this;
  }
}
