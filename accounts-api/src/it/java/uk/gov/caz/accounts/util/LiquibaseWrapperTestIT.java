package uk.gov.caz.accounts.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.accounts.annotation.IntegrationTest;

@IntegrationTest
public class LiquibaseWrapperTestIT {

  @Autowired
  private LiquibaseWrapper liquibaseWrapper;

  @Test
  void canApplyLiquibaseUpdatesUsingWrapperUtility() {
    try {
      liquibaseWrapper.update();
    } catch (LiquibaseException e) {
      fail();
    } catch (SQLException e) {
      fail();
    }
  }
}