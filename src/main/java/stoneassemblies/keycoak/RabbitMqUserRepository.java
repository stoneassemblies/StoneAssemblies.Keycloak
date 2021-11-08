package stoneassemblies.keycoak;

import com.rabbitmq.client.*;
import org.json.JSONArray;
import org.json.JSONObject;
import stoneassemblies.keycoak.interfaces.EncryptionService;
import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class RabbitMqUserRepository implements UserRepository {
    private static Logger logger = Logger.getLogger(JdbcUserStorageProviderFactory.class.getName());

    private final String host;
    private final String virtualHost;
    private final String username;
    private final String password;
    private final long timeout;
    private final EncryptionService encryptionService;
    private final int port;

    private final ConnectionFactory connectionFactory;

    public RabbitMqUserRepository(String host, int port, String virtualHost, String username, String password, long timeout, stoneassemblies.keycoak.interfaces.EncryptionService encryptionService) {
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.timeout = timeout;

        this.encryptionService = encryptionService;
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(this.host);
        this.connectionFactory.setPort(this.port);
        if (this.virtualHost != null && !this.virtualHost.isEmpty()) {
            this.connectionFactory.setVirtualHost(this.virtualHost);
        }

        this.connectionFactory.setUsername(this.username);
        this.connectionFactory.setPassword(this.password);
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

        int count = 0;
        JSONObject response = basicMassTransitRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
        if(response != null && response.has("count")){
            count = response.getInt("count");
        }
        return count;
    }

    private JSONObject basicMassTransitRequest(String queueName, String exchange0, String exchange1, UUID correlationId, String requestMessage) {
        JSONObject response = null;

        String replyQueueName = queueName + "-reply";
        JSONObject sendMessageJsonObject = new JSONObject(requestMessage);
        String requestMessageType = sendMessageJsonObject.getJSONArray("messageType").getString(0);
        try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {

                AtomicReference<JSONObject> receivedMessage = new AtomicReference<>();
                channel.queueDeclare(queueName, true, false, false, null);
                channel.queueDeclare(replyQueueName, true, false, false, null);

                channel.exchangeDeclare(exchange0, BuiltinExchangeType.FANOUT, true);
                channel.exchangeDeclare(exchange1, BuiltinExchangeType.FANOUT, true);

                channel.queueBind(queueName, exchange0, "");
                channel.queueBind(replyQueueName, exchange1, "");

                String consumerTag = channel.basicConsume(replyQueueName, false, (ct, message) -> {
                    if (receivedMessage.get() == null) {
                        try {
                            String source = new String(message.getBody());
                            JSONObject receivedMessageJsonObject = new JSONObject(source);
                            String responseMessageType = receivedMessageJsonObject.getJSONArray("messageType").getString(0);
                            UUID receivedCorrelationId = UUID.fromString(receivedMessageJsonObject.getJSONObject("message").getString("correlationId"));
                            long deliveryTag = message.getEnvelope().getDeliveryTag();
                            if (!requestMessageType.equals(responseMessageType) && receivedCorrelationId.equals(correlationId)) {
                                receivedMessage.set(receivedMessageJsonObject);
                                channel.basicAck(deliveryTag, false);
                            } else {
                                channel.basicNack(deliveryTag, false, true);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, ct -> {

                });

                AMQP.BasicProperties build = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(correlationId.toString())
                        .replyTo(replyQueueName)
                        .build();

                channel.basicPublish(exchange0, queueName, build, requestMessage.getBytes());

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<JSONObject> submit = executor.submit(() -> {
                    while (receivedMessage.get() == null) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        channel.basicCancel(consumerTag);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    JSONObject message = null;
                    JSONObject receivedMessageObject = receivedMessage.get();
                    if(receivedMessageObject.has("message")){
                        message = receivedMessageObject.getJSONObject("message");
                    }

                    return message;
                });

                executor.shutdown();
                response = submit.get(this.timeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
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

        User user = null;
        try {
            JSONObject response = basicMassTransitRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            if(response != null && response.has("user")) {
                JSONObject userJsonObject = response.getJSONObject("user");
                user = getUser(userJsonObject);
            }
        } catch (Exception e) {
            logger.warning(String.format("Error finding user by id '%s'", id));
            e.printStackTrace();
        }

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

        User user = null;
        try {
            JSONObject response = basicMassTransitRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            if(response != null && response.has("user")){
                JSONObject userJsonObject = response.getJSONObject("user");
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

            JSONObject response = basicMassTransitRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            if (response!= null && response.has("succeeded")) {
                succeeded = response.getBoolean("succeeded");
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

            JSONObject response = basicMassTransitRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            if (response != null && response.has("succeeded")) {
                succeeded = response.getBoolean("succeeded");
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

        List<User> userList = new ArrayList<>();
        try {
            JSONObject response = basicMassTransitRequest(queueName, exchange0, exchange1, correlationId, requestMessage);
            if(response != null && response.has("users")){
                JSONArray users = response.getJSONArray("users");
                for (int i = 0; i < users.length(); i++) {
                    User user = getUser(users.getJSONObject(i));
                    if (user != null) {
                        userList.add(user);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning(String.format("Error listing users %s", username));
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
}
