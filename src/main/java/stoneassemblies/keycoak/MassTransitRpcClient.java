package stoneassemblies.keycoak;

import com.rabbitmq.client.*;
import org.json.JSONObject;
import stoneassemblies.keycoak.interfaces.RpcClient;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MassTransitRpcClient implements RpcClient {

    private final SortedSet<UUID> correlationIds = new ConcurrentSkipListSet<>();

    private final SortedMap<UUID, JSONObject> messages = new ConcurrentSkipListMap<>();

    private final ConnectionFactory connectionFactory;

    private final long timeout;

    public MassTransitRpcClient(String host, int port, String virtualHost, String username, String password, long timeout) {
        this.timeout = timeout;
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(host);
        this.connectionFactory.setPort(port);
        if (virtualHost != null && !virtualHost.isEmpty()) {
            this.connectionFactory.setVirtualHost(virtualHost);
        }

        this.connectionFactory.setUsername(username);
        this.connectionFactory.setPassword(password);
    }

    public JSONObject call(String queueName, String requestExchange, String responseExchange, JSONObject requestMessageObject) {
        final UUID correlationId = UUID.randomUUID();
        correlationIds.add(correlationId);

        requestMessageObject.getJSONObject("message").put("correlationId", correlationId.toString());
        String requestMessage = requestMessageObject.toString();

        JSONObject responseMessageObject = null;
        String replyQueueName = queueName + "-reply";
        try (Connection connection = connectionFactory.newConnection(); Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueDeclare(replyQueueName, true, false, false, null);

            channel.exchangeDeclare(requestExchange, BuiltinExchangeType.FANOUT, true);
            channel.exchangeDeclare(responseExchange, BuiltinExchangeType.FANOUT, true);

            channel.queueBind(queueName, requestExchange, "");
            channel.queueBind(replyQueueName, responseExchange, "");

            String consumerTag = channel.basicConsume(replyQueueName, false, (ct, message) -> {
                try {
                    String source = new String(message.getBody());
                    JSONObject receivedMessage = new JSONObject(source).getJSONObject("message");
                    if (receivedMessage.has("correlationId")) {
                        UUID receivedCorrelationId = UUID.fromString(receivedMessage.getString("correlationId"));
                        long deliveryTag = message.getEnvelope().getDeliveryTag();
                        if (correlationIds.contains(receivedCorrelationId)) {
                            channel.basicAck(deliveryTag, false);
                            messages.put(receivedCorrelationId, new JSONObject(source));
                        } else {
                            channel.basicNack(deliveryTag, false, true);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, ct -> {

            });

            AMQP.BasicProperties build = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(correlationId.toString())
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish(requestExchange, queueName, build, requestMessage.getBytes());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<JSONObject> submit = executor.submit(() -> {
                try {
                    while (!messages.containsKey(correlationId)) {
                        Thread.sleep(1);
                    }

                    channel.basicCancel(consumerTag);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                JSONObject message = null;
                JSONObject receivedMessageObject = messages.remove(correlationId);
                if (receivedMessageObject.has("message")) {
                    message = receivedMessageObject.getJSONObject("message");
                }

                return message;
            });

            executor.shutdown();
            responseMessageObject = submit.get(this.timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return responseMessageObject;
    }
}

