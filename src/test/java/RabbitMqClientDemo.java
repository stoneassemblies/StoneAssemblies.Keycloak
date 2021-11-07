import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import stoneassemblies.keycoak.AesEncryptionService;
import stoneassemblies.keycoak.RabbitMqUserRepository;
import stoneassemblies.keycoak.models.User;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class RabbitMqClientDemo {
    @Test
    public void getUsersCount() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        int usersCount = rabbitMqUserRepository.getUsersCount();
    }

    @Test
    public void findUserById() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        User userById = rabbitMqUserRepository.findUserById("123556");
    }

    @Test
    public void findUserByUsernameOrEmail() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        User userById = rabbitMqUserRepository.findUserByUsernameOrEmail("jane.doe0");
    }

    @Test
    public void getUsers() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        List<User> users = rabbitMqUserRepository.getUsers(39, 20);
    }

    @Test
    public void sequentialValidateCredentials() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(rabbitMqUserRepository.validateCredentials("jane.doe" + i, "Password123!"));
        }
    }

    @Test
    public void parallelValidateCredentials() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 5, new AesEncryptionService("sOme*ShaREd*SecreT"));
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            executorService.execute(() -> {
                System.out.println(rabbitMqUserRepository.validateCredentials("jane.doe" + finalI, "Password123!"));
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void updateCredentials() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
//        boolean succeeded = rabbitMqUserRepository.updateCredentials("juan", "Password123!");
    }

    @Test
    public void sendAndReceive() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("syncbee");
        factory.setPassword("syncbee");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            String queueName = "login-queue-3";
//            String callbackQueueName = "login-queue-3-callback";
//            channel.queueDeclare(callbackQueueName, true, false, false, null);

            channel.queueDeclare(queueName, true, false, false, null);
            String exchange0 = "ConsoleApp11:ValueMessage";
            String exchange1 = "ConsoleApp11:WelcomeBack";

            channel.exchangeDeclare(exchange0, BuiltinExchangeType.FANOUT, true);
            channel.exchangeDeclare(exchange1, BuiltinExchangeType.FANOUT, true);

            channel.queueBind(queueName, exchange0, "");
            channel.queueBind(queueName, exchange1, "");

            AtomicBoolean received = new AtomicBoolean(false);

            UUID correlationId = UUID.randomUUID();
            channel.basicConsume(queueName, false, (consumerTag, message) -> {
                try {
                    String s = new String(message.getBody());
                    JSONObject jsonObject = new JSONObject(s);
                    UUID receivedCorrelationId = UUID.fromString(jsonObject.getJSONObject("message").getString("correlationId"));
                    if (receivedCorrelationId.equals(correlationId)) {
                        System.out.println(consumerTag);
                        System.out.println(s);
                        long deliveryTag = message.getEnvelope().getDeliveryTag();
                        channel.basicAck(deliveryTag, false);
                        received.set(true);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, consumerTag -> {
                System.out.println(consumerTag);
            });


            String message = "{\n" +
                    "    \"message\": {\n" +
                    "        \"value\": \"Some Value\",\n" +
                    "        \"correlationId\": \"" + correlationId + "\",\n" +
                    "        \"customerId\": 27\n" +
                    "    },\n" +
                    "    \"messageType\": [\n" +
                    "        \"urn:message:ConsoleApp11:ValueMessage\"\n" +
                    "    ]\n" +
                    "}";

//            String correlationId = UUID.randomUUID().toString();
//            AMQP.BasicProperties build = new AMQP.BasicProperties.Builder()
//                    .replyTo(queueName)
//                    .correlationId(correlationId)
//                    .build();

            channel.basicPublish(exchange0, queueName, null, message.getBytes());

//            RpcClientParams params = new RpcClientParams();
//            params.channel(channel);
//            params.exchange(exchange0);
//            params.routingKey("");
//            params.replyTo(queueName);
//            params.correlationIdSupplier(() -> "AAAAA");
//
//            RpcClient rpcClient = new RpcClient(params);
//            RpcClient.Response response = rpcClient.doCall(null, message.getBytes());

            while (!received.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

