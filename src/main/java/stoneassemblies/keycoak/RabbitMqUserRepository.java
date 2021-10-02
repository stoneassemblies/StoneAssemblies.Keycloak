package stoneassemblies.keycoak;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import stoneassemblies.keycoak.interfaces.EncryptionService;
import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RabbitMqUserRepository implements UserRepository {
    private final String host;
    private final String virtualHost;
    private final String username;
    private final String password;
    private final long timeout;
    private final EncryptionService encryptionService;
    private final int port;

    public RabbitMqUserRepository(String host, int port, String virtualHost, String username, String password, long timeout, stoneassemblies.keycoak.interfaces.EncryptionService encryptionService) {
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
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

        UUID correlationId = UUID.randomUUID();
        String requestMessage = "{\n" +
                "    \"message\": {\n" +
                "        \"correlationId\": \"" + correlationId + "\",\n" +
                "    },\n" +
                "    \"messageType\": [\n" +
                "        \"urn:message:StoneAssemblies.Keycloak.Messages:UsersCountRequestMessage\"\n" +
                "    ]\n" +
                "}";

        JSONObject response = basicRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
        return response.getJSONObject("message").getInt("count");
    }

    private JSONObject basicRequest(String queueName, String exchange0, String exchange1, UUID correlationId, String requestMessage) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
        factory.setPort(this.port);
        if (this.virtualHost != null && !this.virtualHost.isEmpty()) {
            factory.setVirtualHost(this.virtualHost);
        }
        factory.setUsername(this.username);
        factory.setPassword(this.password);

        AtomicReference<JSONObject> receivedMessage = new AtomicReference<>();

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, true, false, false, null);

            channel.exchangeDeclare(exchange0, BuiltinExchangeType.FANOUT, true);
            channel.exchangeDeclare(exchange1, BuiltinExchangeType.FANOUT, true);

            channel.queueBind(queueName, exchange0, "");
            channel.queueBind(queueName, exchange1, "");

            final Object lock = new Object();
            channel.basicConsume(queueName, false, (consumerTag, message) -> {
                synchronized (lock) {
                    if (receivedMessage.get() == null) {
                        try {
                            String source = new String(message.getBody());
                            JSONObject receivedMessageJsonObject = new JSONObject(source);
                            JSONObject sendMessageJsonObject = new JSONObject(requestMessage);
                            String requestMessageType = sendMessageJsonObject.getJSONArray("messageType").getString(0);
                            String responseMessageType = receivedMessageJsonObject.getJSONArray("messageType").getString(0);
                            UUID receivedCorrelationId = UUID.fromString(receivedMessageJsonObject.getJSONObject("message").getString("correlationId"));
                            if (!requestMessageType.equals(responseMessageType) && receivedCorrelationId.equals(correlationId)) {
                                long deliveryTag = message.getEnvelope().getDeliveryTag();
                                receivedMessage.set(receivedMessageJsonObject);
                                channel.basicAck(deliveryTag, false);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }, consumerTag -> {

            });

            channel.basicPublish(exchange0, queueName, null, requestMessage.getBytes());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Integer> submit = executor.submit(() -> {
                while (receivedMessage.get() == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return 0;
            });

            submit.get(this.timeout, TimeUnit.SECONDS);
            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return receivedMessage.get();
    }

    @Override
    public User findUserById(String id) {
        final String queueName = "FindUserByIdRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:FindUserByIdRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:FindUserByIdResponseMessage";

        UUID correlationId = UUID.randomUUID();
        String requestMessage = "{\n" +
                "    \"message\": {\n" +
                "        \"correlationId\": \"" + correlationId + "\",\n" +
                "        \"userId\": \"" + id + "\"\n" +
                "    },\n" +
                "    \"messageType\": [\n" +
                "        \"urn:message:StoneAssemblies.Keycloak.Messages:FindUserByIdRequestMessage\"\n" +
                "    ]\n" +
                "}";

        JSONObject response = basicRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
        JSONObject userJsonObject = response.getJSONObject("message").getJSONObject("user");
        User user = getUser(userJsonObject);
        return user;
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

    @Override
    public User findUserByUsernameOrEmail(String username) {
        final String queueName = "FindUserByUsernameOrEmail";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:FindUserByUsernameOrEmailRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:FindUserByUsernameOrEmailResponseMessage";

        UUID correlationId = UUID.randomUUID();
        String requestMessage = "{\n" +
                "    \"message\": {\n" +
                "        \"correlationId\": \"" + correlationId + "\",\n" +
                "        \"UsernameOrEmail\": \"" + username + "\"\n" +
                "    },\n" +
                "    \"messageType\": [\n" +
                "        \"urn:message:StoneAssemblies.Keycloak.Messages:FindUserByUsernameOrEmailRequestMessage\"\n" +
                "    ]\n" +
                "}";

        JSONObject response = basicRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
        JSONObject message = response.getJSONObject("message");
        JSONObject userJsonObject = message.getJSONObject("user");
        User user = getUser(userJsonObject);
        return user;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        final String queueName = "ValidateCredentialsRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:ValidateCredentialsRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:ValidateCredentialsResponseMessage";

        try {
            password = this.encryptionService.encrypt(password);

            UUID correlationId = UUID.randomUUID();
            String requestMessage = "{\n" +
                    "    \"message\": {\n" +
                    "        \"correlationId\": \"" + correlationId + "\",\n" +
                    "        \"username\": \"" + username + "\",\n" +
                    "        \"password\": \"" + password + "\"\n" +
                    "    },\n" +
                    "    \"messageType\": [\n" +
                    "        \"urn:message:StoneAssemblies.Keycloak.Messages:ValidateCredentialsRequestMessage\"\n" +
                    "    ]\n" +
                    "}";

            JSONObject response = basicRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            JSONObject message = response.getJSONObject("message");
            if (message.has("succeeded")) {
                return message.getBoolean("succeeded");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean updateCredentials(String username, String password) {
        final String queueName = "UpdateCredentialsRequestMessage";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:UpdateCredentialsRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:UpdateCredentialsResponseMessage";

        try {
            password = this.encryptionService.encrypt(password);
            UUID correlationId = UUID.randomUUID();
            String requestMessage = "{\n" +
                    "    \"message\": {\n" +
                    "        \"correlationId\": \"" + correlationId + "\",\n" +
                    "        \"username\": \"" + username + "\",\n" +
                    "        \"password\": \"" + password + "\"\n" +
                    "    },\n" +
                    "    \"messageType\": [\n" +
                    "        \"urn:message:StoneAssemblies.Keycloak.Messages:UpdateCredentialsRequestMessage\"\n" +
                    "    ]\n" +
                    "}";

            JSONObject response = basicRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            JSONObject message = response.getJSONObject("message");
            if (message.has("succeeded")) {
                return message.getBoolean("succeeded");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<User> getUsers(int offset, int take) {
        final String queueName = "FindUserByUsernameOrEmail";
        final String exchange0 = "StoneAssemblies.Keycloak.Messages:UsersRequestMessage";
        final String exchange1 = "StoneAssemblies.Keycloak.Messages:UsersResponseMessage";

        UUID correlationId = UUID.randomUUID();
        String requestMessage = "{\n" +
                "    \"message\": {\n" +
                "        \"correlationId\": \"" + correlationId + "\",\n" +
                "        \"offset\": " + offset + ",\n" +
                "        \"take\": " + take + "\n" +
                "    },\n" +
                "    \"messageType\": [\n" +
                "        \"urn:message:StoneAssemblies.Keycloak.Messages:UsersRequestMessage\"\n" +
                "    ]\n" +
                "}";

        JSONObject response = basicRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
        JSONObject message = response.getJSONObject("message");

        List<User> userList = new ArrayList<>();
        JSONArray users = message.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            User user = getUser(users.getJSONObject(i));
            if (user != null) {
                userList.add(user);
            }
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
}
