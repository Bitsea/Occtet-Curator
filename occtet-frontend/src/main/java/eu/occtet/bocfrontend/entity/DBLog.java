package eu.occtet.bocfrontend.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;


@JmixEntity
@Table(name = "DBLOG")
@Entity
public class DBLog {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    /**
     * Audit-of-creation timestamp.
     * Automatically set by the framework when the entity is first saved.
     */
    @CreatedDate
    @Column(name = "EVENT_DATE", nullable = false, updatable = false)
    private LocalDateTime eventDate;

    @Column(name = "COMPONENT", length = 20)
    private String component;

    /**
     * Makes the log message the “instance name” visible in the UI.
     */
    @InstanceName
    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    public DBLog() {
    }

    public DBLog(String component, String message) {
        this.component = component;
        this.message = message;
    }

    // ——— Getters & setters ———

    public UUID getId() {
        return id;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public String getComponent() {
        return component;
    }
    public void setComponent(String component) {
        this.component = component;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }


    public void setId(UUID id) {
        this.id = id;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }
}