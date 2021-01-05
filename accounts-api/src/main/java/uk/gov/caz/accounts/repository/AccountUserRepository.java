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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
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

  static final String SELECT_ALL_USERS_BY_ACCOUNT_ID_SQL = SELECT_ALL_COLUMNS
      + "FROM caz_account.t_account_user "
      + "WHERE account_id = ?";

  static final String SET_LAST_SIGN_IN_TIMESTMP = "UPDATE caz_account.t_account_user "
      + "SET LAST_SIGN_IN_TIMESTMP = CURRENT_TIMESTAMP "
      + "WHERE account_user_id = ?";

  public static final String DB_SCHEMA_NAME = "caz_account";
  private static final String NOT_UNIQUE_USER_ID_FOR_ACCOUNT_USER_MESSAGE =
      "More than one AccountUser with same userId found";

  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert simpleJdbcInsert;

  /**
   * Creates an instance of {@link AccountUserRepository}.
   *
   * @param jdbcTemplate an instance of {@link JdbcTemplate}
   */
  public AccountUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withTableName("t_account_user")
        .withSchemaName(DB_SCHEMA_NAME)
        .usingGeneratedKeyColumns(Columns.ACCOUNT_USER_ID)
        .usingColumns(Columns.ACCOUNT_ID, Columns.USER_ID, Columns.IS_OWNER,
            Columns.IS_ADMINISTRATED_BY);
  }

  /**
   * Inserts {@code user} into database.
   *
   * @param user An entity object which is supposed to be saved in the database.
   * @return An instance of {@link User} with its internal identifier set.
   * @throws NullPointerException if {@code user} is null
   * @throws NullPointerException if {@link User#getIdentityProviderUserId()} ()} is null
   * @throws NullPointerException if {@link User#getAccountId()} ()} is null
   * @throws IllegalArgumentException if {@link User#getId()} is not null
   */
  public User insert(User user) {
    checkInsertPreconditions(user);

    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(
        toSqlParametersForInsert(user));
    UUID userId = (UUID) keyHolder.getKeys().get(Columns.ACCOUNT_USER_ID);

    return user.toBuilder()
        .id(userId)
        .build();
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
   * Finds list of {@link User} by its {@code accountId} property.
   *
   * @param accountId provided Id of Account assigned to User.
   * @return List of {@link User} with identityProviderUserId set.
   * @throws NullPointerException when {@code userId} is null
   */
  public List<User> findAllUsersByAccountId(UUID accountId) {
    Preconditions.checkNotNull(accountId, "accountId cannot be null");

    return jdbcTemplate.query(SELECT_ALL_USERS_BY_ACCOUNT_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, accountId),
        ROW_MAPPER
    );
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
   * Converts {@code user} into a map of attributes which will be saved in the database.
   */
  private MapSqlParameterSource toSqlParametersForInsert(User user) {
    return new MapSqlParameterSource()
        .addValue(Columns.ACCOUNT_ID, user.getAccountId())
        .addValue(Columns.USER_ID, user.getIdentityProviderUserId())
        .addValue(Columns.IS_OWNER, user.isOwner())
        .addValue(Columns.IS_ADMINISTRATED_BY, user.getAdministeredBy());
  }

  /**
   * Checks if provided user details are valid to be inserted to DB.
   */
  private void checkInsertPreconditions(User user) {
    Preconditions.checkNotNull(user, "User cannot be null");
    Preconditions.checkArgument(user.getId() == null, "User cannot have ID");
    Preconditions.checkNotNull(user.getIdentityProviderUserId(),
        "User need to have identity provider account created");
    Preconditions.checkNotNull(user.getAccountId(), "User need to be assigned to Account");
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
