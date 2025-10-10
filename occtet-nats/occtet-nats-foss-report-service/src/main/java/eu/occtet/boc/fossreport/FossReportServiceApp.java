package eu.occtet.boc.fossreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.fossreport.service.FossReportWorkConsumer;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.service.SystemHandler;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;


@SpringBootApplication
@EnableAsync
@EntityScan("eu.occtet.boc.entity")
@EnableJpaRepositories(basePackages = "eu.occtet.boc.fossreport.dao")
public class FossReportServiceApp {

    @Autowired
    private Connection natsConnection;

    @Autowired
    private FossReportWorkConsumer fossReportWorkConsumer;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    private SystemHandler systemHandler;

    private Executor executor = new SimpleAsyncTaskScheduler();

    private MicroserviceDescriptor microserviceDescriptor;

    private static final Logger log = LoggerFactory.getLogger(FossReportServiceApp.class);


    public static void main(String[] args) {
        SpringApplication.run(FossReportServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);

        log.info("Init Microservice: {} (version {})", microserviceDescriptor.getName(), microserviceDescriptor.getVersion());
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, fossReportWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        executor.execute(()->{
            try {
                fossReportWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, workSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PreDestroy
    public void onShutdown() {
        fossReportWorkConsumer.terminate();
    }

}