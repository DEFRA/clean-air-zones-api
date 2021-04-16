package uk.gov.caz.taxiregister.dto;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;

@Value
@Builder
public class JobFailureData {

  RegisterJobStatus jobStatus;
  Optional<String> ownerEmail;
  List<ValidationError> validationErrors;
}
