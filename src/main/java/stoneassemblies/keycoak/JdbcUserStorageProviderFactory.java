package stoneassemblies.keycoak;

import stoneassemblies.keycoak.constants.JdbcProviderProperties;
import stoneassemblies.keycoak.constants.QueryTypes;
import stoneassemblies.keycoak.constants.SupportedDrivers;
import stoneassemblies.keycoak.interfaces.UserRepository;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class JdbcUserStorageProviderFactory implements UserStorageProviderFactory<UserStorageProvider> {
    private static Logger log = Logger.getLogger(JdbcUserStorageProviderFactory.class.getName());
    @Override
    public UserStorageProvider create(KeycloakSession session, ComponentModel model) {
        UserRepository userRepository = null;
        try {
            String jdbcDriver = model.get(JdbcProviderProperties.JDBC_DRIVER);
            if(SupportedDrivers.MS_SQL_SERVER.equals(jdbcDriver)){
                Class.forName(jdbcDriver);
                String server = model.get(JdbcProviderProperties.SERVER);
                String database = model.get(JdbcProviderProperties.DATABASE);
                String user = model.get(JdbcProviderProperties.USERNAME);
                String password = model.get(JdbcProviderProperties.PASSWORD);
                String connectionString = String.format("jdbc:sqlserver://%s;databaseName=%s;user=%s;password=%s", server, database, user, password);
                String usersQuery = model.get(JdbcProviderProperties.LIST_USERS_QUERY);
                String updatePasswordCommand = model.get(JdbcProviderProperties.UPDATE_PASSWORD_COMMAND);
                String authenticationQuery = model.get(JdbcProviderProperties.AUTHENTICATION_QUERY);
                String authenticationQueryType = model.get(JdbcProviderProperties.AUTHENTICATION_QUERY_TYPE);
                userRepository = new SqlServerUserRepository(connectionString, usersQuery, updatePasswordCommand, authenticationQuery, authenticationQueryType);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        UserStorageProvider userStorageProvider = null;
        if(userRepository != null){
            userStorageProvider = new UserStorageProvider(session, model, userRepository);
        }

        return userStorageProvider;
    }

    @Override
    public String getId() {
        return "jdbc-user-provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ArrayList<String> supportedDriversOptions = new ArrayList<>();
        supportedDriversOptions.addAll(SupportedDrivers.getAll());

        ArrayList<String> authenticationQueryTypes = new ArrayList<>();
        authenticationQueryTypes.addAll(QueryTypes.getAll());

        return ProviderConfigurationBuilder.create()
                .property(JdbcProviderProperties.JDBC_DRIVER, "Jdbc Driver", "Jdbc Driver", ProviderConfigProperty.LIST_TYPE, SupportedDrivers.MS_SQL_SERVER, supportedDriversOptions)
                .property(JdbcProviderProperties.SERVER, "Server", "Server", ProviderConfigProperty.STRING_TYPE, "192.168.1.6", null)
                .property(JdbcProviderProperties.DATABASE, "Database", "Database", ProviderConfigProperty.STRING_TYPE, "Users", null)
                .property(JdbcProviderProperties.USERNAME, "User", "User", ProviderConfigProperty.STRING_TYPE, "sa", null)
                .property(JdbcProviderProperties.PASSWORD, "Password", "Password", ProviderConfigProperty.PASSWORD, "Password123!", null)
                .property(JdbcProviderProperties.LIST_USERS_QUERY, "List Users Query", "List Users Query", ProviderConfigProperty.STRING_TYPE, "SELECT [Id], [Email], [UserName], [FirstName], [LastName], [Password] FROM [dbo].[Users]", null)
                .property(JdbcProviderProperties.AUTHENTICATION_QUERY_TYPE, "Authentication Query Type", "Authentication Query Type", ProviderConfigProperty.LIST_TYPE, QueryTypes.COMMAND_TEXT, authenticationQueryTypes)
                .property(JdbcProviderProperties.AUTHENTICATION_QUERY, "Authentication Query", "Authentication Query", ProviderConfigProperty.STRING_TYPE, "SELECT PATINDEX([Password], ?) AS [Succeeded] FROM [dbo].[Users] WHERE [UserName] = ?", null)
                .property(JdbcProviderProperties.UPDATE_PASSWORD_COMMAND, "Update Password Command", "Update Password Command", ProviderConfigProperty.STRING_TYPE, "UPDATE [dbo].[Users] SET [Password] = ? WHERE [UserName] = ?", null)
                .build();
    }
}

