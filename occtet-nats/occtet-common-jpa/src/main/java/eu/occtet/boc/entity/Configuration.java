package eu.occtet.boc.entity;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "CONFIGURATION")
@EntityListeners(AuditingEntityListener.class)
public class Configuration {

    public enum Type {STRING, NUMERIC, FILE_UPLOAD, BASE_PATH, COMMA_SEPARATED_STRINGS, BOOLEAN}

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "VALUE")
    private String value;

    @Column
    private byte[] upload;

    public Configuration() {
    }

    public Configuration(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte[] getUpload() {
        return upload;
    }

    public void setUpload(byte[] upload) {
        this.upload = upload;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
