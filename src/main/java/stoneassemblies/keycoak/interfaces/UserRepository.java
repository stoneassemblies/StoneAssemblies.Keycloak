package stoneassemblies.keycoak.interfaces;

import stoneassemblies.keycoak.models.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface UserRepository {

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
