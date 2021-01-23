package ch.sebpiller.iot.web.rest;

import ch.sebpiller.iot.service.AmqpPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;

/**
 * REST controller for posting commands.
 */
@RestController
@RequestMapping("/amqp")
public class AmqpResource {
    private final Logger log = LoggerFactory.getLogger(AmqpResource.class);

    private final AmqpPublisher amqpPublisher;

    public AmqpResource(AmqpPublisher amqpPublisher) {
        this.amqpPublisher = amqpPublisher;
    }

    @GetMapping("/status")
    public ResponseEntity<String> get() {
        return ResponseEntity.ok()
            .body("pouet");
    }

    @PostMapping("/command")
    public ResponseEntity<String> postMessage(@RequestBody String message) throws URISyntaxException {
        amqpPublisher.pushCommand(message);

        return ResponseEntity.ok()
            .body(message);
    }
}
