package uk.gov.caz.accounts.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Custom extension for Spring-Data default {@link VehicleChargeabilityRepository}.
 */
@Repository
@AllArgsConstructor
public class VehicleChargeabilityRepositoryImpl implements
    VehicleChargeabilityRepositoryCustom {

  static final String CREATE_TEMP_TABLE =
      "CREATE TEMP TABLE IF NOT EXISTS ids_to_delete_tmp (id uuid)";
  static final String INSERT_IDS_INTO_TEMP_TABLE = "INSERT INTO ids_to_delete_tmp VALUES(?)";
  static final String DELETE_TEMP_TABLE_IDS =
      "DELETE FROM caz_account.t_vehicle_chargeability "
          + "WHERE account_vehicle_id IN (SELECT id FROM ids_to_delete_tmp)";
  static final String DROP_TEMP_TABLE = "DROP TABLE ids_to_delete_tmp";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void deleteFromVehicleChargeability(Set<UUID> ids) {
    if (ids.isEmpty()) {
      return;
    }

    List<Object[]> idsList = ids.stream().map(id -> new Object[]{id})
        .collect(Collectors.toList());

    jdbcTemplate.execute(CREATE_TEMP_TABLE);
    try {
      jdbcTemplate.batchUpdate(INSERT_IDS_INTO_TEMP_TABLE, idsList);
      jdbcTemplate.update(DELETE_TEMP_TABLE_IDS);
    } finally {
      jdbcTemplate.execute(DROP_TEMP_TABLE);
    }
  }
}
