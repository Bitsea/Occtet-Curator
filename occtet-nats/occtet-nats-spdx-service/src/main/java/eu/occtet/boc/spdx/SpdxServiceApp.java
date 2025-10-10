package eu.occtet.boc.spdx;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.WorkerStatus;
import eu.occtet.boc.service.SystemHandler;
import eu.occtet.boc.spdx.service.SpdxWorkConsumer;
import io.nats.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableJpaRepositories(basePackages = {"eu.occtet.boc.spdx.dao"})
@EntityScan(basePackages = {"eu.occtet.boc.entity"})
public class SpdxServiceApp {

    @Autowired
    private Connection natsConnection;

    @Autowired
    private SpdxWorkConsumer spdxWorkConsumer;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    private SystemHandler systemHandler;


    private MicroserviceDescriptor microserviceDescriptor;


    public static void main(String[] args) {
        SpringApplication.run(SpdxServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws JetStreamApiException, IOException {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);

        System.out.println("Init Microservice: " + microserviceDescriptor.getName() + " (version " + microserviceDescriptor.getVersion() + ")");
        // create the systemhandler to respond to "hello", "status" and "exit" messages
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, spdxWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        // start listening for work
        spdxWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(),streamName,workSubject);
    }

    @PreDestroy
    public void onShutdown() {
        spdxWorkConsumer.terminate();
    }
}