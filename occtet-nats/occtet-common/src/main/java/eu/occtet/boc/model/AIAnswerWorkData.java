package eu.occtet.boc.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class AIAnswerWorkData extends BaseWorkData {


    private String answer;

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }

    @JsonCreator
    public AIAnswerWorkData(@JsonProperty("answer")String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
