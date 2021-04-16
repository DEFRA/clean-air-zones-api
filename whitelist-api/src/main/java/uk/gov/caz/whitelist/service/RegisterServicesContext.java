package uk.gov.caz.whitelist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.whitelist.repository.WhitelistedVehicleDtoCsvRepository;

@Component
@lombok.Value
public class RegisterServicesContext {
  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  WhitelistedVehicleDtoToModelConverter dtoToModelConverter;
  WhitelistedVehicleDtoCsvRepository csvRepository;
  int maxValidationErrorCount;

  /**
   * Creates an instance of {@link RegisterServicesContext}.
   */
  public RegisterServicesContext(RegisterService registerService,
      RegisterFromCsvExceptionResolver exceptionResolver,
      RegisterJobSupervisor registerJobSupervisor,
      WhitelistedVehicleDtoToModelConverter dtoToModelConverter, 
      WhitelistedVehicleDtoCsvRepository csvRepository,
      @Value("${application.validation.max-errors-count}") int maxValidationErrorCount) {
    this.registerService = registerService;
    this.exceptionResolver = exceptionResolver;
    this.registerJobSupervisor = registerJobSupervisor;
    this.dtoToModelConverter = dtoToModelConverter;
    this.csvRepository = csvRepository;
    this.maxValidationErrorCount = maxValidationErrorCount;
    
  }
}
