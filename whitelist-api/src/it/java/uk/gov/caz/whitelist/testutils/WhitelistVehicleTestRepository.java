package uk.gov.caz.whitelist.testutils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.caz.whitelist.model.WhitelistVehicle;

@Service
public class WhitelistVehicleTestRepository {

  private static final String FIND_ALL_SQL = "SELECT * FROM caz_whitelist_vehicles.t_whitelist_vehicles";

  static final String DELETE_ALL_SQL = "DELETE FROM caz_whitelist_vehicles.t_whitelist_vehicles";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public void deleteAll() {
    jdbcTemplate.update(DELETE_ALL_SQL);
  }

  public List<WhitelistVehicle> findAll() {
    return jdbcTemplate.query(FIND_ALL_SQL, (rs, rowNum) -> WhitelistVehicle.builder()
        .vrn(rs.getString("vrn"))
        .manufacturer(rs.getString("manufacturer"))
        .reasonUpdated(rs.getString("reason_updated"))
        .updateTimestamp(rs.getObject("update_timestamp", LocalDateTime.class))
        .insertTimestamp(rs.getObject("insert_timestamp", LocalDateTime.class))
        .uploaderId(rs.getObject("uploader_id", UUID.class))
        .uploaderEmail(rs.getString("uploader_email"))
        .build());
  }
}
