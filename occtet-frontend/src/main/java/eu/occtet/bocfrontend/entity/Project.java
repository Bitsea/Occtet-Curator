package eu.occtet.bocfrontend.entity;


import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "PROJECT")
@Entity
public class Project {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @Column(name="PROJECT_NAME")
    private String projectName;


    public Project() {}

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public UUID getId() {return id;}

    public void setId(UUID id) {this.id = id;}

    public String getProjectName() {return projectName;}

    public void setProjectName(String projectName) {this.projectName = projectName;}

}

