package eu.occtet.boc.ai.copyrightFilter.service;



import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.ai.copyrightFilter.dao.InventoryItemRepository;
import eu.occtet.boc.ai.copyrightFilter.factory.AdvisorFactory;
import eu.occtet.boc.ai.copyrightFilter.factory.PromptFactory;
import eu.occtet.boc.ai.copyrightFilter.postprocessing.PostProcessor;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.model.*;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.NatsStreamSender;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
public class LLMService extends BaseWorkDataProcessor {
    private static final Logger log = LogManager.getLogger(LLMService.class);

    @Autowired
    private PromptFactory promptFactory;

    @Autowired
    private PostProcessor postProcessor;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AdvisorFactory advisorFactory;

    @Autowired
    @Qualifier("chatClient")
    private ChatClient chatClient;


    @Autowired
    private Connection natsConnection;

    @Value("${nats.send-subject}")
    private String sendSubject;


    @Bean
    public NatsStreamSender natsStreamSender(){
        return new NatsStreamSender(natsConnection, sendSubject);
    }

    @Override
    public boolean process(AIStatusQueryWorkData workData) {
        log.debug("LLMService: askAI for StatusQuery {}", workData.toString());
        return getAIStatus(workData);
    }

    @Override
    public boolean process(AICopyrightFilterWorkData workData) {
        log.debug("LLMService: askAI for filtering copyrights {}", workData.toString());
        return filterCopyrightsWithAI(workData);
    }


    private boolean getAIStatus(AIStatusQueryWorkData baseWorkData) {
        //MemoryAdvisor is default
        String response = "";
        try {
            response = chatClient.prompt(baseWorkData.getDetails())
                    .call().content();

        } catch (Exception e) {
            log.error("Exception with calling ai {}", e.getMessage());
        }

        postProcessor.deleteThinking(response);
        return  true; // fixme return false on error
    }


    private boolean filterCopyrightsWithAI(AICopyrightFilterWorkData aiWorkData) {
        //MemoryAdvisor is default
        InventoryItem item = getInventoryItem(aiWorkData.getInventoryItemId());
        List<Advisor> advisors = advisorFactory.createAdvisors();
        String response = "";
        Prompt question = promptFactory.createFalseCopyrightPrompt(aiWorkData.getUserMessage());
        String copyrights = createString(aiWorkData.getQuestionableCopyrights());
        String userQuestion = "List of copyrights, separated by |||. Delete invalid copyrights from this list: " + copyrights;

        try {
            response = chatClient.prompt(question).user(userQuestion)
                    .advisors(advisors)
                    .call().content();

        } catch (Exception e) {
            log.error("Exception with calling ai {}", e.getMessage());
        }
        String result = postProcessor.deleteThinking(response);
        handleAIResult(item, result);
        if(!result.isEmpty()) {
            try {
                sendAnswerToStream(result);
                return true;
            } catch (Exception e) {
                log.error("Error when sending message to stream: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * separate single copyrights with |||,so AI can better discern the single ones
     * @param group
     * @return
     */
    private String createString(List<String> group){
        StringBuilder b = new StringBuilder();
        for(String c : group){
            b.append("|||").append(c);
        }
        return b.toString();
    }

    public InventoryItem getInventoryItem(UUID inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId).getFirst();

    }

    /**
     * Handles the AI result and updates the InventoryItem's external notes accordingly.
     * @param item
     * @param response
     */
    public void handleAIResult(InventoryItem item, String response){

        if(item.getExternalNotes()== null &&  !response.isEmpty()){
            item.setExternalNotes(response);
        }else{
            item.setExternalNotes(item.getExternalNotes()+"\n"+response);
        }
        inventoryItemRepository.save(item);
    }

    /**
     * Sends the AI-generated answer to the NATS stream for further processing.
     * @param answer
     * @throws JetStreamApiException
     * @throws IOException
     */
    private void sendAnswerToStream(String answer) throws JetStreamApiException, IOException {

        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("status_request", "question", actualTimestamp, new AIAnswerWorkData(answer));
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(workTask);
        log.debug("sending message to ai service: {}", message);
        natsStreamSender().sendWorkMessageToStream( message.getBytes(Charset.defaultCharset()));
    }
}
