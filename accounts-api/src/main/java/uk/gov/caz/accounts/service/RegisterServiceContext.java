package uk.gov.caz.accounts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.repository.AccountVehicleDtoCsvRepository;
import uk.gov.caz.accounts.service.chargecalculation.AsyncChargeCalculationStarter;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;
import uk.gov.caz.accounts.service.registerjob.RegisterService;

@Component
@lombok.Value
public class RegisterServiceContext {

  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  AccountVehicleDtoCsvRepository csvRepository;
  AccountVehicleDtoToModelConverter dtoToModelConverter;
  AsyncChargeCalculationStarter asyncChargeCalculationStarter;
  int maxValidationErrorCount;

  /**
   * Creates an instance of {@link RegisterServiceContext}.
   */
  public RegisterServiceContext(RegisterService registerService,
      RegisterFromCsvExceptionResolver exceptionResolver,
      RegisterJobSupervisor registerJobSupervisor,
      AccountVehicleDtoCsvRepository csvRepository,
      AccountVehicleDtoToModelConverter dtoToModelConverter,
      AsyncChargeCalculationStarter asyncChargeCalculationStarter,
      @Value("${application.validation.max-errors-count}") int maxValidationErrorCount) {
    this.registerService = registerService;
    this.exceptionResolver = exceptionResolver;
    this.registerJobSupervisor = registerJobSupervisor;
    this.csvRepository = csvRepository;
    this.dtoToModelConverter = dtoToModelConverter;
    this.asyncChargeCalculationStarter = asyncChargeCalculationStarter;
    this.maxValidationErrorCount = maxValidationErrorCount;
  }
}
