package eu.occtet.boc.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name="INFORMATION_FILE",uniqueConstraints = { @UniqueConstraint(columnNames = { "FILE_NAME"}) })
public class InformationFile {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    @Column(name= "FILE_NAME")
    private String fileName;

    @Column(name= "FILE_PATH")
    private String filePath;

    @Column(name="FILE_CONTEXT")
    private String context;

    @Column(name="FILEINFORMATION_CONTENT", columnDefinition = "TEXT")
    private String content;

    public InformationFile(){}

    public InformationFile(String filename, String context, String content, String path) {
        this.fileName = filename;
        this.context = context;
        this.content = content;
        this.filePath= path;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
