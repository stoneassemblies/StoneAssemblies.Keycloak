package stoneassemblies.keycoak;

import stoneassemblies.keycoak.constants.QueryTypes;
import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.Query;
import stoneassemblies.keycoak.models.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SqlServerUserRepository implements UserRepository {
    private static Logger log = Logger.getLogger(JdbcUserStorageProviderFactory.class.getName());

    private final String connectionString;
    private final Query usersQuery;
    private final Query updateCredentialsCommand;
    private final Query authenticationQuery;

    public SqlServerUserRepository(String connectionString,
                                   Query usersQuery,
                                   Query updateCredentialsQuery,
                                   Query validateCredentialsQuery) {
        this.connectionString = connectionString;
        this.usersQuery = usersQuery; // TODO: Validate for security reason.
        this.updateCredentialsCommand = updateCredentialsQuery;
        this.authenticationQuery = validateCredentialsQuery;
    }

    public static User mapRow(ResultSet resultSet, int i) throws SQLException {
        User user = new User();

        user.setId(resultSet.getString("Id"));
        user.setUsername(resultSet.getString("UserName"));
        user.setEmail(resultSet.getString("Email"));

        // Optional columns.
        try {
            user.setFirstName(resultSet.getString("FirstName"));
        } catch (SQLException exception) {
            user.setFirstName("");
        }

        try {
            user.setLastName(resultSet.getString("LastName"));
        } catch (SQLException exception) {
            user.setLastName("");
        }

        try {
            user.setPassword(resultSet.getString("Password"));
        } catch (SQLException exception) {
        }

        try {
            user.setEnabled(resultSet.getBoolean("IsEnabled"));
        } catch (SQLException exception) {
            user.setEnabled(true);
        }

        try {
            user.setEmailVerified(resultSet.getBoolean("IsEmailVerified"));
        } catch (SQLException exception) {
        }

        String commaSeparatedRoles = null;
        try {
             commaSeparatedRoles = resultSet.getString("Roles");
        } catch (SQLException exception) {
        }

        if (commaSeparatedRoles != null) {
            List<String> roles = Arrays.stream(commaSeparatedRoles.split(",")).map(s -> s.trim()).filter(s -> !s.equals("")).collect(Collectors.toList());
            user.setRoles(roles);
        } else {
            user.setRoles(new ArrayList<>());
        }

        user.setCreated(System.currentTimeMillis());
        return user;
    }


    @Override
    public List<User> getAllUsers() {
        // TODO: Fix this ?
//        try {
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(this.connectionString, false));
//            return jdbcTemplate.query(usersQuery.getText(), SqlServerUserRepository::mapRow);
//        } catch (DataAccessException e) {
//            e.printStackTrace();
//        }

        return new ArrayList<>();
    }

    @Override
    public int getUsersCount() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(this.connectionString, false));
            return jdbcTemplate.query(String.format("SELECT COUNT(*) FROM (%s) T", this.usersQuery), resultSet -> resultSet.next() ?
                    resultSet.getInt(1) : 0);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public User findUserById(String id) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(this.connectionString, false));
            return jdbcTemplate.queryForObject(String.format("SELECT * FROM (%s) T WHERE Id=?", this.usersQuery.getText()),
                    SqlServerUserRepository::mapRow, new Object[]{id});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public User findUserByUsernameOrEmail(String username) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(this.connectionString, false));
            return jdbcTemplate.queryForObject(String.format("SELECT * FROM (%s) T WHERE UserName=? OR Email=?", this.usersQuery.getText()),
                    SqlServerUserRepository::mapRow, new Object[]{username, username});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        boolean succeeded = false;
        if (this.authenticationQuery != null && !this.authenticationQuery.getText().trim().equals("")) {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
                if (this.authenticationQuery.getQueryType().equals(QueryTypes.COMMAND_TEXT)) {
                    log.info("Validating credentials using command text");
                    succeeded = jdbcTemplate.query(this.authenticationQuery.getText(), preparedStatement -> {
                        preparedStatement.setString(1, password);
                        preparedStatement.setString(2, username);
                    }, resultSet -> resultSet.next() && resultSet.getBoolean("succeeded") && !resultSet.next());
                } else {
                    log.info("Validating credentials using stored procedure");
                    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName(this.authenticationQuery.getText());
                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                            .addValue("username", username)
                            .addValue("password", password);
                    Map<String, Object> execute = simpleJdbcCall.execute(mapSqlParameterSource);
                    if(execute != null){
                        List<Map<String, Object>> maps = (List<Map<String, Object>>) execute.get("#result-set-1");
                        Map<String, Object> stringObjectMap = maps.stream().findFirst().get();
                        if(stringObjectMap != null){
                            Object result = stringObjectMap.values().stream().findFirst().get();
                            if(result instanceof Integer){
                                succeeded = (int) result == 1;
                            }
                            else if(result instanceof Boolean)
                            {
                                succeeded = (boolean) result;
                            }
                        }
                    }
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
        if (this.updateCredentialsCommand != null && !this.updateCredentialsCommand.getText().trim().equals("")) {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
                if (this.updateCredentialsCommand.getQueryType().equals(QueryTypes.COMMAND_TEXT)) {
                    log.info("Updating credentials using command text");

                    jdbcTemplate.execute(this.updateCredentialsCommand.getText(), (PreparedStatementCallback<Object>) preparedStatement -> {
                        preparedStatement.setString(1, password);
                        preparedStatement.setString(2, username);
                        return preparedStatement.execute();
                    });
                }
                else {
                    log.info("Updating  credentials using stored procedure");

                    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName(this.updateCredentialsCommand.getText());
                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                            .addValue("username", username)
                            .addValue("password", password);

                    simpleJdbcCall.execute(mapSqlParameterSource);
                }

                return true;
            } catch (DataAccessException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public List<User> getUsers(int offset, int take) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connectionString, false));
            return jdbcTemplate.query(String.format("SELECT * FROM (%s) T ORDER BY Id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", usersQuery.getText()),
                    SqlServerUserRepository::mapRow, new Object[]{new Integer(offset), new Integer(take)});
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
            return jdbcTemplate.query(String.format("SELECT TOP 0 * FROM (%s) T WHERE UserName LIKE ? OR Email LIKE ? OR FirstName LIKE ? OR LastName LIKE ?", usersQuery.getText()),
                    SqlServerUserRepository::mapRow, new Object[]{searchPattern, searchPattern, searchPattern, searchPattern});
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
            return jdbcTemplate.query(String.format("SELECT COUNT(*) FROM (%s) T WHERE UserName LIKE ? OR Email LIKE ? OR FirstName LIKE ? OR LastName LIKE ?", usersQuery.getText()), resultSet -> resultSet.next() ?
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
                    SqlServerUserRepository::mapRow, new Object[]{searchPattern, searchPattern, searchPattern, searchPattern, new Integer(offset), new Integer(fetch)});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }
}
