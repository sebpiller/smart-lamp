package ch.sebpiller.iot.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Properties specific to Smartlampweb.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
    private ConnectionFactory amqpConnectionFactory;

    @Bean
    public ConnectionFactory getAmqpConnectionFactory() {
        return amqpConnectionFactory;
    }

    public void setAmqpConnectionFactory(ConnectionFactory amqpConnectionFactory) {
        this.amqpConnectionFactory = amqpConnectionFactory;
    }
}
