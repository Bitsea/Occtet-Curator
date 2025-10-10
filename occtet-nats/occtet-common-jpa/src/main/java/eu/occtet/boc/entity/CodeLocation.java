package eu.occtet.boc.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "CODE_LOCATION")
@EntityListeners(AuditingEntityListener.class)
public class CodeLocation {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "INVENTORYITEM_ID")
    private InventoryItem inventoryItem;

    @Column(name = "FILE_PATH", columnDefinition="TEXT")
    private String filePath;


    @Column(name = "LINE_NUMBER")
    private Integer lineNumberOne;

    @Column(name = "LINE_NUMBER_TO")
    private Integer lineNumberTwo;


    public CodeLocation(String filePath, Integer lineNumberOne, Integer lineNumberTwo) {

        this.filePath= filePath;
        this.lineNumberOne = lineNumberOne;
        this.lineNumberTwo = lineNumberTwo;
    }

    public CodeLocation(String filePath) {
        this.filePath= filePath;
    }

    public CodeLocation(InventoryItem inventoryItem, String filePath) {
        this.inventoryItem = inventoryItem;
        this.filePath = filePath;
    }

    public CodeLocation() {}

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

    public void setLineNumberOne(Integer lineNumberOne) {
        this.lineNumberOne = lineNumberOne;
    }
}