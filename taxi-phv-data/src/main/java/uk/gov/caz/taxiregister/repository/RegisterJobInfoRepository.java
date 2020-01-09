package uk.gov.caz.taxiregister.repository;

import com.google.common.annotations.VisibleForTesting;
import java.sql.PreparedStatement;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.LicensingAuthority;

/**
 * Class that manages updating T_MD_REGISTER_JOBS_INFO table.
 */
@AllArgsConstructor
@Repository
@Slf4j
public class RegisterJobInfoRepository {

  @VisibleForTesting
  static final String INSERT_SQL = "insert "
      + "into T_MD_REGISTER_JOBS_INFO (register_job_id, licence_authority_id) "
      + "values (?, ?)";

  private final JdbcTemplate jdbcTemplate;

  /**
   * Inserts IDs from the passed {@code affectedLicensingAuthorities} into T_MD_REGISTER_JOBS_INFO
   * table.
   *
   * @param registerJobId The ID of a registered finished job which caused updating licenses
   *     from {@code affectedLicensingAuthorities} licensing authorities.
   * @param affectedLicensingAuthorities Licensing authorities whose licences were updated.
   */
  public void insert(int registerJobId, Set<LicensingAuthority> affectedLicensingAuthorities) {
    Integer[] licensingAuthoritiesIds = toArray(affectedLicensingAuthorities);
    jdbcTemplate.update(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
      preparedStatement.setInt(1, registerJobId);
      preparedStatement.setArray(2,
          connection.createArrayOf("integer", licensingAuthoritiesIds));
      return preparedStatement;
    });
  }

  /**
   * Maps a set of {@link LicensingAuthority} to an array of licensing authorities IDs.
   */
  private Integer[] toArray(Set<LicensingAuthority> affectedLicensingAuthorities) {
    return affectedLicensingAuthorities.stream()
        .map(LicensingAuthority::getId)
        .toArray(Integer[]::new);
  }
}
