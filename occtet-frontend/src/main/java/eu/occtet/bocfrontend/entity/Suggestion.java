package eu.occtet.bocfrontend.entity;


import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name="SUGGESTION" )
@Entity
public class Suggestion {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @Column(name= "CONTEXT")
    private String context;

    @Column(name= "SENTENCE")
    private String sentence;

    public Suggestion() {
    }

    public Suggestion(String context, String sentence) {
        this.context = context;
        this.sentence = sentence;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }
}
