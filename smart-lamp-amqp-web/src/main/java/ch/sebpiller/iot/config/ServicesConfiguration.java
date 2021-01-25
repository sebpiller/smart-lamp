package ch.sebpiller.iot.config;

import ch.sebpiller.iot.service.AmqpPublisher;
import ch.sebpiller.iot.service.impl.AmqpPublisherImpl;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServicesConfiguration {
    @Bean
    public AmqpPublisher amqpPublisher(ConnectionFactory cf) {
        AmqpPublisherImpl amqpPublisher = new AmqpPublisherImpl(cf);
        return amqpPublisher;
    }

    @Bean
    public ConnectionFactory getAmqpConnectionFactory(ApplicationProperties applicationProperties) {
        return applicationProperties.getAmqpConnectionFactory();
    }
}
