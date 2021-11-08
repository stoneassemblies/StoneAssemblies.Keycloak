package stoneassemblies.keycoak;

import org.json.JSONArray;
import org.json.JSONObject;
import stoneassemblies.keycoak.interfaces.EncryptionService;
import stoneassemblies.keycoak.interfaces.RpcClient;
import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class RabbitMqUserRepository implements UserRepository {

    private static final String EMPTY_REQUEST_MESSAGE_TEMPLATE = "{ \"message\": { }, \"messageType\": [ ] }";

    private static Logger logger = Logger.getLogger(JdbcUserStorageProviderFactory.class.getName());

    private final EncryptionService encryptionService;

    private final RpcClient rpcClient;

    public RabbitMqUserRepository(String host, int port, String virtualHost, String username, String password, long timeout, EncryptionService encryptionService) {
        this(RpcClientFactory.create(host, port, virtualHost, username, password, timeout), encryptionService);
    }

    public RabbitMqUserRepository(RpcClient rpcClient, EncryptionService encryptionService) {
        this.rpcClient = rpcClient;
        this.encryptionService = encryptionService;
    }

    @Override
    public List<User> getAllUsers() {
        return this.getUsers(0, this.getUsersCount());
    }

    @Override
    public int getUsersCount() {
        final String queueName = "UsersCountRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:UsersCountRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:UsersCountResponseMessage";

        JSONObject requestMessageObject = new JSONObject(EMPTY_REQUEST_MESSAGE_TEMPLATE);
        requestMessageObject.getJSONArray("messageType").put(0, "urn:message:StoneAssemblies.Keycloak.Messages:UsersCountRequestMessage");

        int count = 0;
        JSONObject responseMessageObject = rpcClient.call(queueName, exchange0, exchange1, requestMessageObject);
        if (responseMessageObject != null && responseMessageObject.has("count")) {
            count = responseMessageObject.getInt("count");
        }
        return count;
    }


    @Override
    public User findUserById(String id) {
        final String queueName = "FindUserByIdRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:FindUserByIdRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:FindUserByIdResponseMessage";

        JSONObject requestMessageObject = new JSONObject(EMPTY_REQUEST_MESSAGE_TEMPLATE);
        requestMessageObject.getJSONObject("message").put("userId", id);
        requestMessageObject.getJSONArray("messageType").put(0, "urn:message:StoneAssemblies.Keycloak.Messages:FindUserByIdRequestMessage");

        User user = null;
        try {
            JSONObject responseMessageObject = rpcClient.call(queueName, exchange0, exchange1, requestMessageObject);
            if (responseMessageObject != null && responseMessageObject.has("user")) {
                JSONObject userJsonObject = responseMessageObject.getJSONObject("user");
                user = getUser(userJsonObject);
            }
        } catch (Exception e) {
            logger.warning(String.format("Error finding user by id '%s'", id));
            e.printStackTrace();
        }

        return user;
    }

    @Override
    public User findUserByUsernameOrEmail(String username) {
        final String queueName = "FindUserByUsernameOrEmail";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:FindUserByUsernameOrEmailRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:FindUserByUsernameOrEmailResponseMessage";

        JSONObject requestMessageObject = new JSONObject(EMPTY_REQUEST_MESSAGE_TEMPLATE);
        requestMessageObject.getJSONObject("message").put("UsernameOrEmail", username);
        requestMessageObject.getJSONArray("messageType").put(0, "urn:message:StoneAssemblies.Keycloak.Messages:FindUserByUsernameOrEmailRequestMessage");

        User user = null;
        try {
            JSONObject responseMessageObject = rpcClient.call(queueName, exchange0, exchange1, requestMessageObject);
            if (responseMessageObject != null && responseMessageObject.has("user")) {
                JSONObject userJsonObject = responseMessageObject.getJSONObject("user");
                user = getUser(userJsonObject);
            }
        } catch (Exception e) {
            logger.warning(String.format("Error finding user by name '%s'", username));
            e.printStackTrace();
        }

        return user;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        final String queueName = "ValidateCredentialsRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:ValidateCredentialsRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:ValidateCredentialsResponseMessage";

        Boolean succeeded = false;
        try {
            password = this.encryptionService.encrypt(password);

            JSONObject requestMessageObject = new JSONObject(EMPTY_REQUEST_MESSAGE_TEMPLATE);
            requestMessageObject.getJSONObject("message").put("username", username);
            requestMessageObject.getJSONObject("message").put("password", password);
            requestMessageObject.getJSONArray("messageType").put(0, "urn:message:StoneAssemblies.Keycloak.Messages:ValidateCredentialsRequestMessage");

            JSONObject responseMessageObject = rpcClient.call(queueName, exchange0, exchange1, requestMessageObject);
            if (responseMessageObject != null && responseMessageObject.has("succeeded")) {
                succeeded = responseMessageObject.getBoolean("succeeded");
            }
        } catch (Exception e) {
            logger.warning(String.format("Error validating credentials of user %s", username));
            e.printStackTrace();
        }

        return succeeded;
    }

    @Override
    public boolean updateCredentials(String username, String password) {
        final String queueName = "UpdateCredentialsRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:UpdateCredentialsRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:UpdateCredentialsResponseMessage";

        Boolean succeeded = false;
        try {
            password = this.encryptionService.encrypt(password);

            JSONObject requestMessageObject = new JSONObject(EMPTY_REQUEST_MESSAGE_TEMPLATE);
            requestMessageObject.getJSONObject("message").put("username", username);
            requestMessageObject.getJSONObject("message").put("password", password);
            requestMessageObject.getJSONArray("messageType").put(0, "urn:message:StoneAssemblies.Keycloak.Messages:UpdateCredentialsRequestMessage");

            JSONObject responseMessageObject = rpcClient.call(queueName, exchange0, exchange1, requestMessageObject);
            if (responseMessageObject != null && responseMessageObject.has("succeeded")) {
                succeeded = responseMessageObject.getBoolean("succeeded");
            }
        } catch (Exception e) {
            logger.warning(String.format("Error updating credentials of user %s", username));
            e.printStackTrace();
        }

        return succeeded;
    }

    @Override
    public List<User> getUsers(int offset, int take) {
        final String queueName = "FindUserByUsernameOrEmail";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:UsersRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:UsersResponseMessage";

        JSONObject requestMessageObject = new JSONObject(EMPTY_REQUEST_MESSAGE_TEMPLATE);
        requestMessageObject.getJSONObject("message").put("offset", offset);
        requestMessageObject.getJSONObject("message").put("take", take);
        requestMessageObject.getJSONArray("messageType").put(0, "urn:message:StoneAssemblies.Keycloak.Messages:UsersRequestMessage");

        List<User> userList = new ArrayList<>();
        try {
            JSONObject response = rpcClient.call(queueName, exchange0, exchange1, requestMessageObject);
            if (response != null && response.has("users")) {
                JSONArray users = response.getJSONArray("users");
                for (int i = 0; i < users.length(); i++) {
                    User user = getUser(users.getJSONObject(i));
                    if (user != null) {
                        userList.add(user);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning(String.format("Error listing users"));
            e.printStackTrace();
        }

        return userList;
    }

    @Override
    public List<User> findUsers(String search) {
        return Collections.emptyList();
    }

    @Override
    public int getUsersCount(String search) {
        return 0;
    }

    @Override
    public List<User> findUsers(String search, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    private User getUser(JSONObject userJsonObject) {
        if (!userJsonObject.has("id") || !userJsonObject.has("username")) {
            return null;
        }

        User user = new User();
        user.setId(userJsonObject.getString("id"));
        user.setUsername(userJsonObject.getString("username"));
        user.setEnabled(userJsonObject.has("enabled") && userJsonObject.getBoolean("enabled"));
        user.setCreated(System.currentTimeMillis());

        if (userJsonObject.has("firstName")) {
            user.setFirstName(userJsonObject.getString("firstName"));
        }

        if (userJsonObject.has("lastName")) {
            user.setLastName(userJsonObject.getString("lastName"));
        }

        if (userJsonObject.has("email")) {
            user.setEmail(userJsonObject.getString("email"));
        }

        return user;
    }
}
