package ch.sebpiller.iot.lamp.sequencer.luke.roberts.lamp.f;

import ch.sebpiller.iot.lamp.sequencer.SmartLampScript;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class AmqpIntegrationCliTest {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpIntegrationCliTest.class);
    private Channel channel;
    private String queueName;
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq.home");
        factory.setUsername("lampf");
        factory.setPassword("spidybox");
        factory.setPort(5672);

        LOG.info("starting RabbitMq queue listening mode...");

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare("lampf", true, false, false, null);
        channel.exchangeDeclare("command", "direct", true);

        queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "command", "push");

    }


    @After
    public void tearDown() throws Exception {
        channel.close();
        connection.close();
    }

    @Test
    public void testCreateMessages() throws IOException {
        String com = "color=emerald;";

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