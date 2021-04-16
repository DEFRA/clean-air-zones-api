package uk.gov.caz.tariff.util;

import java.sql.SQLException;

import liquibase.exception.LiquibaseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.tariff.annotation.IntegrationTest;

@IntegrationTest
public class LiquibaseWrapperTestIT {

  @Autowired
  private LiquibaseWrapper liquibaseWrapper;

  @Test
  void canApplyLiquibaseUpdatesUsingWrapperUtility() {
    try {
      liquibaseWrapper.update();
    } catch (LiquibaseException e) {
      Assertions.fail();
    } catch (SQLException e) {
      Assertions.fail();
    }
  }
} 