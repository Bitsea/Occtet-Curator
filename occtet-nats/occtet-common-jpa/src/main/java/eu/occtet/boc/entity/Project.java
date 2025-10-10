package eu.occtet.boc.entity;


import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "PROJECT")
@EntityListeners(AuditingEntityListener.class)
public class Project {


    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    @Column(name="PROJECT_NAME")
    private String projectName;

    public Project() {
    }

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
