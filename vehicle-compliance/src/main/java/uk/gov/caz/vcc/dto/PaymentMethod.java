package uk.gov.caz.vcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Returns one of the following values: CARD, DIRECT_DEBIT, NULL.
 */
public enum PaymentMethod {

  /**
   * Paid by credit or debit card.
   */
  @JsonProperty("card")
  CARD,
  /**
   * Paid by direct debit.
   */
  @JsonProperty("direct_debit")
  DIRECT_DEBIT,
  /**
   * Not yet paid or unable to determine payment method.
   */
  @JsonProperty("null")
  NULL;

  /**
   * Returns String representation as required by use cases. For "NULL" enum value returns null.
   *
   * @return String representation used by DTO use cases. Returns null for "NULL" enum value.
   */
  public String toDtoString() {
    switch (this) {
      case CARD:
        return "card";
      case DIRECT_DEBIT:
        return "direct_debit";
      default:
        return null;
    }
  }
}
