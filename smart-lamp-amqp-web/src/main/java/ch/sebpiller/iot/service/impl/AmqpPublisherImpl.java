package ch.sebpiller.iot.service.impl;

import ch.sebpiller.iot.service.AmqpPublisher;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service Implementation for publishing data to AMQP.
 */
@Service
public class AmqpPublisherImpl implements AmqpPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpPublisherImpl.class);

    public static final String QUEUE = "lampf";
    public static final String EXCHANGE = "command";
    public static final String ROUTING_KEY = "push";

    private final ConnectionFactory factory;

    @Autowired
    public AmqpPublisherImpl(ConnectionFactory connectionFactory) {
        this.factory = connectionFactory;
    }

    @Override
    public void pushCommand(String command) {
        LOG.info("receiving command to push to AMQP: {}", command);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE, false, false, false, null);
            channel.exchangeDeclare(EXCHANGE, "direct", false);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE, ROUTING_KEY);

            channel.basicPublish(EXCHANGE, ROUTING_KEY,
                new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2)
                    .priority(1)
                    .userId(factory.getUsername())
                    .build(),
                command.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.error("exception", e);
        }
    }
}
