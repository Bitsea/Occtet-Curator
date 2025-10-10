package eu.occtet.bocfrontend.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "CONFIGURATION")
@Entity
public class Configuration {

    public enum Type {STRING, NUMERIC, FILE_UPLOAD, BASE_PATH, BOOLEAN}

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "UPLOAD", length =1000000000)
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

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {this.name = name;}

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public byte[] getUpload() {
        return upload;
    }

    public void setUpload(byte[] upload) {
        this.upload = upload;
    }

}
