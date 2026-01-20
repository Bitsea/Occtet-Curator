package eu.occtet.boc.entity;

import eu.occtet.boc.converter.StringListConverter;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "IMPORT_TASK", indexes = {
        @Index(columnList = "IMPORT_NAME"),
        @Index(columnList = "STATUS")
})
@EntityListeners(AuditingEntityListener.class)
public class ImportTask {


    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Column(name = "IMPORT_NAME", nullable = false)
    private String importName;

    @Column(name = "STATUS", nullable = false)
    private String status;

    // Jmix cannot handle List<String> as a column type. Therefore a converter is needed.
    @Convert(converter = StringListConverter.class)
    @Column(name = "FEEDBACK", columnDefinition = "TEXT")
    private List<String> feedback;

    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "IMPORT_TASK_ID")
    private List<Configuration> importConfiguration;

    @Column(name = "LAST_UPDATE")
    private @Nullable LocalDateTime lastUpdate;

    public ImportTask() {
        status = ImportStatus.CREATING.getId();
    }

    public ImportTask(String name, Project project) {
        status = ImportStatus.CREATING.getId();
        this.importName = name;
        this.project = project;
    }


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImportName() {
        return importName;
    }

    public void setImportName(String importName) {
        this.importName = importName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<String> feedback) {
        this.feedback = feedback;
    }

    public List<Configuration> getImportConfiguration() {
        return importConfiguration;
    }

    public void setImportConfiguration(List<Configuration> importConfiguration) {
        this.importConfiguration = importConfiguration;
    }

    @Nullable
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(@Nullable LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

}
