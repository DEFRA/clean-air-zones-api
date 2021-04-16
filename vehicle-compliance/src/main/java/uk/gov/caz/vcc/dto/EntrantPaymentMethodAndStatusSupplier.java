package uk.gov.caz.vcc.dto;

public interface EntrantPaymentMethodAndStatusSupplier {

  PaymentStatus getPaymentStatus();

  PaymentMethod getPaymentMethod();
}
