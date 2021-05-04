package stoneassemblies.keycoak;

import stoneassemblies.keycoak.constants.QueryTypes;
import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class SqlServerUserRepository implements UserRepository {
    private static Logger log = Logger.getLogger(JdbcUserStorageProviderFactory.class.getName());

    private final String authenticationQueryType;
    private final String connectionString;
    private final String usersQuery;
    private final String updateCredentialsCommand;
    private final String authenticationQueryOrStoredProcedureName;

    public SqlServerUserRepository(String connectionString, String usersQuery, String updateCredentialsCommand,
                                   String authenticationQueryOrStoredProcedureName, String authenticationQueryType) {
        this.connectionString = connectionString;
        this.usersQuery = usersQuery; // TODO: Validate for security reason.
        this.updateCredentialsCommand = updateCredentialsCommand;
        this.authenticationQueryOrStoredProcedureName = authenticationQueryOrStoredProcedureName;
        this.authenticationQueryType = authenticationQueryType;
    }




    @Override
    public List<User> getAllUsers() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.query(usersQuery, UserRepository::mapRow);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    @Override
    public int getUsersCount() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.query(String.format("SELECT COUNT(*) FROM (%s) T", usersQuery), resultSet -> resultSet.next() ?
                    resultSet.getInt(1) : 0);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public User findUserById(String id) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.queryForObject(String.format("SELECT * FROM (%s) T WHERE Id=?", usersQuery),
                    UserRepository::mapRow, new Object[]{id});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public User findUserByUsernameOrEmail(String username) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.queryForObject(String.format("SELECT * FROM (%s) T WHERE UserName=? OR Email=?", usersQuery),
                    UserRepository::mapRow, new Object[]{username, username});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        boolean succeeded;
        if (authenticationQueryOrStoredProcedureName != null && !authenticationQueryOrStoredProcedureName.trim().equals("")) {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
                if (this.authenticationQueryType.equals(QueryTypes.COMMAND_TEXT)) {
                    log.info("Validating credentials using command text");
                    succeeded = jdbcTemplate.query(authenticationQueryOrStoredProcedureName, preparedStatement -> {
                        preparedStatement.setString(1, password);
                        preparedStatement.setString(2, username);
                    }, resultSet -> resultSet.next() && resultSet.getBoolean("Succeeded") && !resultSet.next());
                } else {
                    log.info("Validating credentials using stored procedure");
                    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName(this.authenticationQueryOrStoredProcedureName);
                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                            .addValue("username", username)
                            .addValue("password", password);
                    Map<String, Object> execute = simpleJdbcCall.execute(mapSqlParameterSource);
                    succeeded = (boolean) execute.get("Succeeded");
                }
            } catch (Exception e) {
                succeeded = false;
                e.printStackTrace();
            }
        } else {
            log.info("Validating credentials with empty command");
            succeeded = findUserByUsernameOrEmail(username).getPassword().equals(password);
        }

        return succeeded;
    }

    @Override
    public boolean updateCredentials(String username, String password) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            jdbcTemplate.execute(updateCredentialsCommand, (PreparedStatementCallback<Object>) preparedStatement -> {
                preparedStatement.setString(1, password);
                preparedStatement.setString(2, username);
                return preparedStatement.execute();
            });

            return true;
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<User> getUsers(int offset, int take) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.query(String.format("SELECT * FROM (%s) T ORDER BY Id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", usersQuery),
                    UserRepository::mapRow, new Object[]{new Integer(offset), new Integer(take)});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }


    @Override
    public List<User> findUsers(String search) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            String searchPattern = String.format("%%%s%%", search);
            return jdbcTemplate.query(String.format("SELECT TOP 0 * FROM (%s) T WHERE UserName LIKE ? OR Email LIKE ? OR FirstName LIKE ? OR LastName LIKE ?", usersQuery),
                    UserRepository::mapRow, new Object[]{searchPattern, searchPattern, searchPattern, searchPattern});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    @Override
    public int getUsersCount(String search) {
        try {
            String searchPattern = String.format("%%%s%%", search);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.query(String.format("SELECT COUNT(*) FROM (%s) T WHERE UserName LIKE ? OR Email LIKE ? OR FirstName LIKE ? OR LastName LIKE ?", usersQuery), resultSet -> resultSet.next() ?
                    resultSet.getInt(1) : 0, new Object[]{searchPattern, searchPattern, searchPattern, searchPattern});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public List<User> findUsers(String search, int offset, int fetch) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            String searchPattern = String.format("%%%s%%", search);
            return jdbcTemplate.query(String.format("SELECT * FROM (%s) T WHERE UserName LIKE ? OR Email LIKE ? OR FirstName LIKE ? OR LastName LIKE ? ORDER BY Id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", usersQuery),
                    UserRepository::mapRow, new Object[]{searchPattern, searchPattern, searchPattern, searchPattern, new Integer(offset), new Integer(fetch)});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }
}
