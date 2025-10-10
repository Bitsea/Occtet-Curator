package eu.occtet.boc.service;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending NATS messaging.
 */

public class NatsStreamSender {

    private static final Logger log = LoggerFactory.getLogger(NatsStreamSender.class);


    private Connection natsConnection;


    private String subjectName;


    public NatsStreamSender(Connection natsConnection, String subjectName) {
        this.natsConnection = natsConnection;
        this.subjectName = subjectName;
    }

    /**
     * Sends a work message to the specified NATS stream (i.e. "work.taskname")
     * @param message
     * @throws JetStreamApiException
     * @throws IOException
     */
    public void sendWorkMessageToStream(byte[] message) throws JetStreamApiException, IOException {
        natsConnection.jetStream().publish(subjectName,message);
    }


}
