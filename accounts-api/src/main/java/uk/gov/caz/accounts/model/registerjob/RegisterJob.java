package uk.gov.caz.accounts.model.registerjob;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.caz.accounts.model.registerjob.converter.RegisterJobErrorsConverter;
import uk.gov.caz.accounts.model.registerjob.converter.RegisterJobNameConverter;

/**
 * A database entity which represents a registered job.
 */
@Entity
@Table(schema = "caz_account", name = "t_account_job_register")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegisterJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "register_job_id")
  Integer id;

  @NonNull
  @Column(name = "job_name")
  @Convert(converter = RegisterJobNameConverter.class)
  RegisterJobName jobName;

  @NonNull
  @Column(name = "uploader_id")
  UUID uploaderId;

  @NonNull
  @Enumerated(EnumType.STRING)
  RegisterJobTrigger trigger;

  @NonNull
  @Enumerated(EnumType.STRING)
  RegisterJobStatus status;

  @NonNull
  @Convert(converter = RegisterJobErrorsConverter.class)
  RegisterJobErrors errors;

  @NonNull
  @Column(name = "correlation_id")
  String correlationId;

  @Column(name = "last_modified_timestmp")
  @UpdateTimestamp
  LocalDateTime lastModifiedTimestamp;
}
