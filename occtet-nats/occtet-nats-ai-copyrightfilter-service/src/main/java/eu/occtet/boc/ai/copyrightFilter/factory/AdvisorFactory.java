package eu.occtet.boc.ai.copyrightFilter.factory;

import eu.occtet.boc.ai.copyrightFilter.retriever.CopyrightRetriever;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdvisorFactory {
    @Autowired
    private CopyrightRetriever copyrightRetriever;


    public List<Advisor> createAdvisors(){
        List<Advisor> advisors = new ArrayList<>();

        //TODO figure out where we get topK and similarityThreshold from and find suitable values for our usecases
        //advisor for "good copyrights"
        advisors.add(copyrightRetriever.buildQuestionAnswerAdvisor(10, 0.25, "Examples of good copyrights: ", "fileName == 'good-copyrights.txt'"));
        //advisor for "bad copyrights"
        advisors.add(copyrightRetriever.buildQuestionAnswerAdvisor(10, 0.25, "Examples of bad copyrights: ", "fileName == 'bad-copyrights.txt'"));

        return advisors;
    }
}
