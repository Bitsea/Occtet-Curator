package eu.occtet.boc.ai.copyrightFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.ai.copyrightFilter.service.AICopyrightFilterWorkConsumer;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.service.SystemHandler;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = "eu.occtet.boc.entity")
@EnableJpaRepositories(basePackages = "eu.occtet.boc.ai.copyrightFilter.dao")
public class AICopyrightFilterServiceApp {

    @Autowired
    private Connection natsConnection;

    private MicroserviceDescriptor microserviceDescriptor;

    @Autowired
    private AICopyrightFilterWorkConsumer aiCopyrightFilterWorkConsumer;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    private SystemHandler systemHandler;

    private Executor executor = new SimpleAsyncTaskScheduler();

    private static final Logger log = LogManager.getLogger(AICopyrightFilterServiceApp.class);

    public static void main(String[] args) {
        SpringApplication.run(AICopyrightFilterServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);

        log.info("Init Microservice: {} (version {})", microserviceDescriptor.getName(), microserviceDescriptor.getVersion());
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, aiCopyrightFilterWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        executor.execute(()->{
            try {
                aiCopyrightFilterWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, workSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PreDestroy
    public void onShutdown() {
        aiCopyrightFilterWorkConsumer.terminate();
    }


}
