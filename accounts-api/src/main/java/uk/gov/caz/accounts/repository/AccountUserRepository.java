package uk.gov.caz.accounts.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.exception.NotUniqueUserIdForAccountUserException;

/**
 * A class which handles managing data in {@code ACCOUNT_USER} table.
 */
@Repository
@Slf4j
public class AccountUserRepository {

  private static final UserRowMapper ROW_MAPPER = new UserRowMapper();

  static final String SELECT_ALL_COLUMNS =
      "SELECT " + Columns.USER_ID + ", " + Columns.ACCOUNT_USER_ID + ", " + Columns.ACCOUNT_ID
          + ", " + Columns.IS_ADMINISTRATED_BY + ", " + Columns.IS_OWNER + " ";

  @VisibleForTesting
  static final String SELECT_BY_USER_ID_SQL = SELECT_ALL_COLUMNS
      + "FROM caz_account.t_account_user "
      + "WHERE user_id=?";

  static final String SELECT_BY_ID_SQL = SELECT_ALL_COLUMNS
      + "FROM caz_account.t_account_user "
      + "WHERE account_user_id = ?";

  private static final String NOT_UNIQUE_USER_ID_FOR_ACCOUNT_USER_MESSAGE =
      "More than one AccountUser with same userId found";

  private final JdbcTemplate jdbcTemplate;

  /**
   * Creates an instance of {@link AccountUserRepository}.
   *
   * @param jdbcTemplate an instance of {@link JdbcTemplate}
   */
  public AccountUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Fetches record from ACCOUNT_USER table with provided userId.
   *
   * @param userId provided Id assigned to user by third-party Identity Provider
   * @return An instance of {@link User} with identityProviderUserId set.
   * @throws NullPointerException when {@code userId} is null
   */
  public Optional<User> findByUserId(UUID userId) {
    Preconditions.checkNotNull(userId, "userId cannot be null");

    List<User> results = jdbcTemplate
        .query(SELECT_BY_USER_ID_SQL,
            preparedStatement -> preparedStatement.setObject(1, userId),
            ROW_MAPPER);

    if (results.size() > 1) {
      throw new NotUniqueUserIdForAccountUserException(
          NOT_UNIQUE_USER_ID_FOR_ACCOUNT_USER_MESSAGE);
    }
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * Finds {@link User} by its {@code id} property (mapped to ACCOUNT_USER_ID column in the DB).
   */
  public Optional<User> findById(UUID id) {
    List<User> results = jdbcTemplate.query(SELECT_BY_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, id),
        ROW_MAPPER
    );

    return results.stream().findFirst();
  }

  /**
   * A class that maps the row returned from the database to an instance of {@link User}.
   */
  static class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet resultSet, int i) throws SQLException {
      UUID identityProviderUserId = resultSet.getString(Columns.USER_ID) == null
          ? null
          : resultSet.getObject(Columns.USER_ID, UUID.class);

      return User.builder()
          .identityProviderUserId(identityProviderUserId)
          .id(UUID.fromString(resultSet.getString(Columns.ACCOUNT_USER_ID)))
          .accountId(resultSet.getObject(Columns.ACCOUNT_ID, UUID.class))
          .isOwner(resultSet.getBoolean(Columns.IS_OWNER))
          .administeredBy(resultSet.getObject(Columns.IS_ADMINISTRATED_BY, UUID.class))
          .build();
    }
  }

  /**
   * Private class to carry sql table columns.
   */
  private static class Columns {

    private static final String USER_ID = "user_id";
    private static final String ACCOUNT_USER_ID = "account_user_id";
    private static final String ACCOUNT_ID = "account_id";
    private static final String IS_OWNER = "is_owner";
    private static final String IS_ADMINISTRATED_BY = "is_administrated_by";
  }
}
