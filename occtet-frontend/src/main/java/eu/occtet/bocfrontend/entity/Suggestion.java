package eu.occtet.bocfrontend.entity;


import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


@JmixEntity
@Table(name="SUGGESTION" )
@Entity
public class Suggestion implements HasOrganization {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private Long id;

    @Column(name= "CONTEXT")
    private String context;

    @Column(name= "SENTENCE")
    private String sentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZATION_ID")
    private Organization organization;

    public Suggestion() {
    }

    public Suggestion(String context, String sentence) {
        this.context = context;
        this.sentence = sentence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
