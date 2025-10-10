package eu.occtet.bocfrontend.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.util.UUID;

@JmixEntity
@Table(name = "CODE_LOCATION")
@Entity
public class CodeLocation {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "INVENTORYITEM_ID")
    private InventoryItem inventoryItem;


    @Column(name = "FILE_PATH", columnDefinition = "Text")
    private String filePath;

    @Column(name= "LINE_NUMBER")
    private Integer lineNumberOne;

    @Column(name= "LINE_NUMBER_TO")
    private Integer lineNumberTwo;

    public CodeLocation(){}

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getLineNumberOne() {
        return lineNumberOne;
    }

    public void setLineNumberOne(Integer lineNumberOne) {
        this.lineNumberOne = lineNumberOne;
    }

    public Integer getLineNumberTwo() {
        return lineNumberTwo;
    }

    public void setLineNumberTwo(Integer lineNumberTwo) {
        this.lineNumberTwo = lineNumberTwo;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
}