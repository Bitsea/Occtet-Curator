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
@Table(name = "SCANNER_INITIALIZER", indexes = {
        @Index(columnList = "SCANNER"),
        @Index(columnList = "STATUS")
})
@Entity
public class ScannerInitializer {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVENTORY_ITEM_ID", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "SCANNER", nullable = false)
    private String scanner;

    @Column(name = "STATUS", nullable = false)
    private String status;

    // Jmix cannot handle List<String> as a column type. Therefore a converter is needed.
    @Convert(converter = ListStringConverter.class)
    @Column(name = "FEEDBACK", columnDefinition = "TEXT")
    private List<String> feedback;

    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "SCANNER_INITIALIZER_ID")
    private List<Configuration> scannerConfiguration;

    @Column(name = "LAST_UPDATE")
    private @Nullable LocalDateTime lastUpdate;

    public ScannerInitializer() {
        status = ScannerInitializerStatus.CREATING.getId();
    }

    public ScannerInitializer(String scanner, InventoryItem inventoryItem) {
        status = ScannerInitializerStatus.CREATING.getId();
        this.scanner= scanner;
        this.inventoryItem = inventoryItem;
    }

    public String getScanner() {
        return scanner;
    }

    public void setScanner(String scanner) {
        this.scanner = scanner;
    }

    public String getStatus() {
        return status;
    }

    public void updateStatus(String status) {
        this.status = status;
        this.lastUpdate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ScannerInitializer{" +
                "id=" + id +
                ", scanner='" + scanner + '\'' +
                ", status=" + status +
                '}';
    }

    public List<Configuration> getScannerConfiguration() {
        return scannerConfiguration;
    }

    public void setScannerConfiguration(List<Configuration> scannerConfiguration) {
        this.scannerConfiguration = scannerConfiguration;
    }

    @Nullable
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(@Nullable LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public UUID getId() {return id;}

    public void setId(UUID id) {this.id = id;}

    public void setStatus(String status) {this.status = status;}

    public List<String> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<String> feedback) {
        this.feedback = feedback;
    }

    public InventoryItem getInventoryItem()
    {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
}
