import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
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
import java.util.concurrent.atomic.AtomicInteger;

public class RabbitMqClientTests {

    @Test
    @Ignore
    public void getUsersCount() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        int usersCount = rabbitMqUserRepository.getUsersCount();
        Assert.assertNotEquals(0, usersCount);
    }

    @Test
    @Ignore
    public void findUserById() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        User user = rabbitMqUserRepository.findUserByUsernameOrEmail("jane.doe0");
        User userById = rabbitMqUserRepository.findUserById(user.getId());
        Assert.assertNotNull(userById);
    }

    @Test
    @Ignore
    public void findUserByUsernameOrEmail() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        User user = rabbitMqUserRepository.findUserByUsernameOrEmail("jane.doe0");
        Assert.assertNotNull(user);
    }

    @Test
    @Ignore
    public void getUsers() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        List<User> users = rabbitMqUserRepository.getUsers(39, 20);
        Assert.assertNotEquals(0, users.size());
        for (User user : users) {
            System.out.println(user.getUsername());
        }
    }

    @Test
    @Ignore
    public void sequentialValidateCredentials() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        for (int i = 0; i < 5; i++) {
            Assert.assertTrue(rabbitMqUserRepository.validateCredentials("jane.doe" + i, "Password123!"));
        }
    }

    @Test
    @Ignore
    public void parallelValidateCredentials() {
        long startTime = System.nanoTime();
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 60, new AesEncryptionService("sOme*ShaREd*SecreT"));
        ExecutorService executorService = Executors.newCachedThreadPool();
        AtomicInteger count = new AtomicInteger();
        int expected = 100;
        for (int i = 0; i < expected; i++) {
            int finalI = i;
            executorService.execute(() -> {
                if(rabbitMqUserRepository.validateCredentials("jane.doe" + finalI, "Password123!")) {
                    count.getAndIncrement();
                }
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println(duration + "ms");

        Assert.assertEquals(expected, count.get());
    }

    @Test
    @Ignore
    public void updateCredentials() {
        RabbitMqUserRepository rabbitMqUserRepository = new RabbitMqUserRepository("localhost", 6002, "public", "queuedemo", "queuedemo", 10, new AesEncryptionService("sOme*ShaREd*SecreT"));
        boolean succeeded = rabbitMqUserRepository.updateCredentials("juan", "Password123!");
    }
}

