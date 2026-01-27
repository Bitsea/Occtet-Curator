package eu.occtet.bocfrontend.entity;

import eu.occtet.boc.converter.ListStringConverter;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@JmixEntity
@Table(name = "CURATOR_TASK", indexes = {
        @Index(columnList = "TASK_NAME"),
        @Index(columnList = "STATUS")
})
@Entity
public class CuratorTask {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Column(name = "TASK_NAME", nullable = false)
    @InstanceName
    private String taskName;

    @Column(name="TASK_TYPE", nullable = false)
    private String taskType;

    @Column(name = "STATUS", nullable = false)
    private TaskStatus status;

    // Jmix cannot handle List<String> as a column type. Therefore a converter is needed.
    @Column(name = "FEEDBACK", columnDefinition = "TEXT")
    private String feedback;

    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "TASK_ID")
    private List<Configuration> taskConfiguration;

    @Column(name = "LAST_UPDATE")
    private @Nullable LocalDateTime lastUpdate;

    @Column(name = "START_DATE")
    private @Nullable LocalDateTime startDate;

    @Column(name = "PROGRESS")
    private Integer progress=0;

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    @Nullable
    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public CuratorTask() {
        status = TaskStatus.CREATING;
        progress = 0;
    }

    public CuratorTask(String name, Project project, String taskType) {
        this.taskType = taskType;
        status = TaskStatus.CREATING;
        taskName = name;
        this.project = project;
        progress = 0;
    }

    public void notifyStarted() {
        status = TaskStatus.IN_PROGRESS;
        startDate = LocalDateTime.now();
        progress=0;
    }

    public void setCurrentProgress(int p) {
        if(p>=100){
             setCompleted();
             return;
        }
        status = TaskStatus.IN_PROGRESS;
        progress = p;
        lastUpdate = LocalDateTime.now();
    }

    public void setCompleted() {
        status = TaskStatus.COMPLETED;
        progress = 100;
        lastUpdate = LocalDateTime.now();
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

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        lastUpdate = LocalDateTime.now();
    }

    public List<String> getFeedback() {
        return ListStringConverter.nullableStringToList(feedback);
    }

    public void setFeedback(List<String> feedback) {
        this.feedback = ListStringConverter.toStringOrNull(feedback);
        lastUpdate = LocalDateTime.now();
    }

    public List<Configuration> getTaskConfiguration() {
        return taskConfiguration;
    }

    public void setTaskConfiguration(List<Configuration> taskConfiguration) {
        this.taskConfiguration = taskConfiguration;
    }

    @Nullable
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(@Nullable LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
}
