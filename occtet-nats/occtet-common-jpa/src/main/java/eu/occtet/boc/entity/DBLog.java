package eu.occtet.boc.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "DBLOG")
@EntityListeners(AuditingEntityListener.class)
public class DBLog {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    // Should automatically set the current timestamp on creation
    @Column(name = "EVENT_DATE", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime eventDate;

    @Column(name = "COMPONENT", length = 20)
    private String component;

    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    // Constructors
    public DBLog() {
    }

    public DBLog(String component, String message) {
        this.component = component;
        this.message = message;
    }

    // Getters and Setters

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
}
