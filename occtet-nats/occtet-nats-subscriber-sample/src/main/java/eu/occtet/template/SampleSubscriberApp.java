package eu.occtet.template;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class SampleSubscriberApp {

  @Autowired
  private Connection natsConnection;

  @Value("${nats.subject}")
  private String subject;

  public static void main(String[] args) {
      SpringApplication.run(SampleSubscriberApp.class, args);
  }


  @PostConstruct
  public void onInit() {
    subscribeToSubject(subject);
  }

  public void subscribeToSubject(String subject) {
    Subscription subscription = natsConnection.subscribe(subject);
    try {
      while(true) {
        Message message = subscription.nextMessage(1000);
        if (message != null) {
          String msg = new String(message.getData(), StandardCharsets.UTF_8);
          System.out.println("Received message: " + msg);
        }
      }
    } catch (InterruptedException e) {

    }



  }


}