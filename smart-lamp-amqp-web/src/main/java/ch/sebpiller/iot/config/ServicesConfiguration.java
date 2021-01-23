package ch.sebpiller.iot.config;

import ch.sebpiller.iot.service.AmqpPublisher;
import ch.sebpiller.iot.service.impl.AmqpPublisherImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServicesConfiguration {
    @Bean
    public AmqpPublisher amqpPublisher(ApplicationProperties applicationProperties) {
        AmqpPublisherImpl amqpPublisher = new AmqpPublisherImpl(applicationProperties.getAmqpConnectionFactory());
        return amqpPublisher;
    }
}
