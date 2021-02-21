package ch.sebpiller.iot.lamp.sequencer.luke.roberts.lamp.f;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AmqpIntegrationCliTest {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpIntegrationCliTest.class);
    private Channel channel;
    private String queueName;
    private Connection connection;


    private ConnectionFactory getConnectionFactory() {
        return new Yaml().loadAs(getClass().getResourceAsStream("/config/amqp.rabbitmq.home.yaml"), ConnectionFactory.class);
    }

    @BeforeEach
    public void setUp() throws Exception {
        ConnectionFactory factory = getConnectionFactory();

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare("lampf", false, false, false, null);
        channel.exchangeDeclare("command", "direct", false);

        queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "command", "push");
    }

    @AfterEach
    public void tearDown() throws Exception {
        channel.close();
        connection.close();
    }

    @Test
    public void testCreateMessages() throws IOException {
        String com = "scene=5";

        //for (int i = 0; i < 50; i++) {
        channel.basicPublish("command", "push",
                new AMQP.BasicProperties.Builder()
                        .contentType("text/plain")
                        .deliveryMode(2)
                        .priority(1)
                        .userId("lampf")
                        .build(),
                com.getBytes(StandardCharsets.UTF_8));
        // }
    }
}