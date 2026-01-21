import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.issueCatcher.service.IssueCatcherService;
import eu.occtet.boc.issueCatcher.service.IssueCatcherWorkConsumer;
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
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@SpringBootApplication(scanBasePackages = {"eu.occtet.boc"})
@EnableAsync
@EntityScan(basePackages = "eu.occtet.boc.entity")
@EnableJpaRepositories(basePackages = "eu.occtet.boc.issueCatcher.dao")
@Profile({"!test"})
public class IssueCatcherServiceApp {

    @Autowired
    private Connection natsConnection;

    private MicroserviceDescriptor microserviceDescriptor;

    @Autowired
    private IssueCatcherWorkConsumer issueCatcherWorkConsumer;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    @Value("${application.version}")
    private String applicationVersion;

    private SystemHandler systemHandler;

    private Executor executor = new SimpleAsyncTaskScheduler();

    private static final Logger log = LoggerFactory.getLogger(IssueCatcherServiceApp.class);

    public static void main(String[] args) {
        SpringApplication.run(IssueCatcherServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);
        microserviceDescriptor.setVersion(applicationVersion);
        log.info("Init Microservice: {} (version {})", microserviceDescriptor.getName(), microserviceDescriptor.getVersion());
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, issueCatcherWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        executor.execute(()->{
            try {
                issueCatcherWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, workSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PostConstruct
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownApplication));
    }

    private void shutdownApplication() {
        System.out.println("shutting down Microservice: " + microserviceDescriptor.getName() );
        issueCatcherWorkConsumer.terminate();
        Runtime.getRuntime().halt(0);
    }

}
