package ch.sebpiller.iot.service;

public interface AmqpPublisher {
    void pushCommand(String command);
}
