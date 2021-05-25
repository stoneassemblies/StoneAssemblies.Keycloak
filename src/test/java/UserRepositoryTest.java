import stoneassemblies.keycoak.models.Query;
import stoneassemblies.keycoak.constants.QueryTypes;
import stoneassemblies.keycoak.models.User;
import stoneassemblies.keycoak.SqlServerUserRepository;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;


public class UserRepositoryTest {

    @Test
    @Ignore
    public void basic() throws ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        //SqlServerUserRepository userRepository = new SqlServerUserRepository("jdbc:sqlserver://localhost;databaseName=Users;user=sa;password=Password123!", "SELECT [Id]\n" +
        SqlServerUserRepository userRepository = new SqlServerUserRepository("jdbc:sqlserver://192.168.1.6:11433;databaseName=Users;user=sa;password=Password123!", new Query("SELECT [Id]\n" +
                "      ,[UserName]\n" +
                "      ,[Email]\n" +
                "      ,[FirstName]\n" +
                "      ,[LastName]\n" +
                "      ,[Password]\n" +
                "  FROM [dbo].[Users]"), null, null);

        User userById = userRepository.findUserById("1");
    }

    @Test
    @Ignore
    public void validateCredentials() throws ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        //SqlServerUserRepository userRepository = new SqlServerUserRepository("jdbc:sqlserver://localhost;databaseName=Users;user=sa;password=Password123!", "SELECT [Id]\n" +
        SqlServerUserRepository userRepository = new SqlServerUserRepository("jdbc:sqlserver://localhost:11433;databaseName=Users;user=sa;password=Password-123!", new Query("SELECT [Id]\n" +
                "      ,[UserName]\n" +
                "      ,[Email]\n" +
                "      ,[FirstName]\n" +
                "      ,[LastName]\n" +
                "      ,[Password]\n" +
                "  FROM [dbo].[Users]"), null,
                new Query("SELECT PATINDEX([Password], ?) AS [Succeeded] FROM [dbo].[Users] WHERE [UserName] = ?"));

        boolean alexfdezsauco = userRepository.validateCredentials("alexfdezsauco", "Password12345!");

    }

    @Test
    @Ignore
    public void validateCredentialsViaStoredProcedure() throws ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        SqlServerUserRepository userRepository = new SqlServerUserRepository(
                "jdbc:sqlserver://localhost:11433;databaseName=Users;user=sa;password=Password-123!",
                null,
                null,
                new Query("validateCredentials", QueryTypes.STORED_PROCEDURE));

        userRepository.validateCredentials("alexfdezsauco", "Password12345!");
    }

    @Test
    @Ignore
    public void basic2() throws ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        SqlServerUserRepository userRepository = new SqlServerUserRepository("jdbc:sqlserver://localhost:11433;databaseName=Users;user=sa;password=Password-123!", new Query("SELECT [Id]\n" +
                "      ,[UserName]\n" +
                "      ,[Email]\n" +
                "      ,[FirstName]\n" +
                "      ,[LastName]\n" +
             //   "      ,[Password]\n" +
                "  FROM [dbo].[Users]"), null, null);

        List<User> users = userRepository.getUsers(0, 1);

        for (User user: users) {
            System.out.println(user.getEmail());
        }
    }
}