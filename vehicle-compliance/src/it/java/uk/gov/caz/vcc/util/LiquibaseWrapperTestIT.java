package uk.gov.caz.vcc.util;

import java.sql.SQLException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import liquibase.exception.LiquibaseException;
import uk.gov.caz.vcc.annotation.IntegrationTest;


@IntegrationTest
public class LiquibaseWrapperTestIT {

  @Autowired
  private LiquibaseWrapper liquibaseWrapper;
  
  @Test
  void canApplyLiquibaseUpdatesUsingWrapperUtility() {
    try {
      liquibaseWrapper.update();
    } catch (LiquibaseException e) {
      Assert.fail();
    } catch (SQLException e) {
      Assert.fail();
    }
  }

}
