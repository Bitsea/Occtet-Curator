package eu.occtet.boc.ai.copyrightFilter.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {


    private static final Logger log = LogManager.getLogger(ChatClientConfig.class);

    /**
     * very basic config of the Chatclient, most config is done in the Promptfactory as of now
     * @param builder
     * @return
     */
    @Bean(name= "chatClient")
    public ChatClient chatClient(ChatClient.Builder builder) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        MessageChatMemoryAdvisor chatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory);
        return builder.defaultSystem("You provide answers given on provided information. Do not think too much.")
                .defaultAdvisors(chatMemoryAdvisor).build();
    }


}
