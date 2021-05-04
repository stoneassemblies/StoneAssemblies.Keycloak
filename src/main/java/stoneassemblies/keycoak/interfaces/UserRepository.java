package stoneassemblies.keycoak.interfaces;

import stoneassemblies.keycoak.models.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface UserRepository {
    static User mapRow(ResultSet resultSet, int i) throws SQLException {
        User user = new User();

        user.setId(resultSet.getString("Id"));
        user.setUsername(resultSet.getString("UserName"));
        user.setEmail(resultSet.getString("Email"));

        // Optional columns.
        try {
            user.setFirstName(resultSet.getString("FirstName"));
        } catch (SQLException exception) {
        }

        try {
            user.setLastName(resultSet.getString("LastName"));
        } catch (SQLException exception) {
        }

        try {
            user.setPassword(resultSet.getString("Password"));
        } catch (SQLException exception) {
        }

        user.setEnabled(true);
        user.setCreated(System.currentTimeMillis());
        return user;
    }

    List<User> getAllUsers();

    int getUsersCount();

    User findUserById(String id);

    User findUserByUsernameOrEmail(String username);

    boolean validateCredentials(String username, String password);

    boolean updateCredentials(String username, String password);

    List<User> getUsers(int offset, int take);

    List<User> findUsers(String search);

    int getUsersCount(String search);

    List<User> findUsers(String search, int firstResult, int maxResults);
}
