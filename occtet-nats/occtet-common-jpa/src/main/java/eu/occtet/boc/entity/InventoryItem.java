package eu.occtet.boc.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "INVENTORY_ITEM")
@EntityListeners(AuditingEntityListener.class)
public class InventoryItem {


    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    @Column(name="INVENTORY_NAME")
    private String inventoryName;

    @Column(name= "SIZE")
    private Integer size;

    @Column(name= "SPDX_ID")
    private String spdxId;

    @Column(name= "LINKING")
    private String linking;

    @Column (name= "PRIORITY")
    private Integer priority;

    @Column(name= "CONSPICUOUS")
    private Boolean conspicuous;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "INVENTORY_ITEM_ID")
    private List<Copyright> copyrights;

    @Column(name= "EXTERNAL_NOTES", columnDefinition = "TEXT")
    private String externalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_INVENTORY_ITEM_ID")
    private InventoryItem parent;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "SOFTWARE_COMPONENT_ID", nullable = true)
    private SoftwareComponent softwareComponent;

    @Column(name= "WAS_COMBINED")
    private Boolean wasCombined;

    @Column(name= "CURATED")
    private Boolean curated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Column(name = "BASEPATH")
    private String basePath;

    @Column(name = "CREATED_AT", updatable = false)
    private @Nonnull LocalDateTime createdAt;

    public InventoryItem() {
        this.createdAt = LocalDateTime.now();
    }

    public InventoryItem(
            String inventoryName,
            int size,
            String linking,
            List<Copyright> copyrights,
            String externalNotes,
            InventoryItem parent,
            SoftwareComponent softwareComponent,
            boolean wasCombined,
            boolean curated,
            Project project,
            String basePath, String spdxId
    ) {
        this.createdAt = LocalDateTime.now();
        this.inventoryName = inventoryName;
        this.size = size;
        this.linking = linking;
        this.copyrights = copyrights;
        this.externalNotes = externalNotes;
        this.parent = parent;
        this.softwareComponent = softwareComponent;
        this.wasCombined = wasCombined;
        this.curated = curated;
        this.project = project;
        this.basePath = basePath;
        this.spdxId = spdxId;
    }

    public InventoryItem(String inventoryName, Project project, SoftwareComponent softwareComponent) {
        this.createdAt = LocalDateTime.now();
        this.inventoryName = inventoryName;
        this.project = project;
        this.softwareComponent = softwareComponent;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInventoryName() {
        return inventoryName;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }


    public String getLinking() {
        return linking;
    }

    public void setLinking(String linking) {
        this.linking = linking;
    }


    public String getExternalNotes() {
        return externalNotes;
    }

    public void setExternalNotes(String externalNotes) {
        this.externalNotes = externalNotes;
    }

    public InventoryItem getParent() {
        return parent;
    }

    public void setParent(InventoryItem parent) {
        this.parent = parent;
    }

    public SoftwareComponent getSoftwareComponent() {
        return softwareComponent;
    }

    public void setSoftwareComponent(SoftwareComponent softwareComponent) {
        this.softwareComponent = softwareComponent;
    }

    public boolean isWasCombined() {
        return wasCombined;
    }

    public void setWasCombined(boolean wasCombined) {
        this.wasCombined = wasCombined;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }


    public List<Copyright> getCopyrights() {
        return copyrights;
    }

    public void setCopyrights(List<Copyright> copyrights) {
        this.copyrights = copyrights;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSpdxId() {
        return spdxId;
    }

    public void setSpdxId(String spdxId) {
        this.spdxId = spdxId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean isConspicuous() {
        return conspicuous;
    }

    public void setConspicuous(Boolean conspicuous) {
        this.conspicuous = conspicuous;
    }

    public Boolean getWasCombined() {
        return wasCombined;
    }

    public void setWasCombined(Boolean wasCombined) {
        this.wasCombined = wasCombined;
    }

    public Boolean getCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public void setCreatedAt(@Nonnull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
