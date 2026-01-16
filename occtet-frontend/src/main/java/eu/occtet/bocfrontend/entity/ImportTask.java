package eu.occtet.bocfrontend.entity;

import eu.occtet.bocfrontend.converter.ListStringConverter;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "IMPORTER", indexes = {
        @Index(columnList = "NAME"),
        @Index(columnList = "STATUS")
})
@Entity
public class ImportTask {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Column(name = "IMPORT_NAME", nullable = false)
    private String importName;

    @Column(name = "STATUS", nullable = false)
    private String status;

    // Jmix cannot handle List<String> as a column type. Therefore a converter is needed.
    @Convert(converter = ListStringConverter.class)
    @Column(name = "FEEDBACK", columnDefinition = "TEXT")
    private List<String> feedback;

    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "IMPORTER_ID")
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
