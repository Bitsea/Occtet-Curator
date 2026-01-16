/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "INVENTORY_ITEM")
@EntityListeners(AuditingEntityListener.class)
public class InventoryItem {


    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Column(name="INVENTORY_NAME", length=1024)
    private String inventoryName;

    @Column(name= "SIZE")
    private Integer size;

    @Column(name= "SPDX_ID", length=512)
    private String spdxId;

    @Column(name= "LINKING")
    private String linking;

    @Column (name= "PRIORITY")
    private Integer priority;

    @Column(name= "CONSPICUOUS")
    private Boolean conspicuous;

    @Column(name= "EXTERNAL_NOTES",length = Integer.MAX_VALUE)
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

    @Column(name = "CREATED_AT", updatable = false)
    private @Nonnull LocalDateTime createdAt;

    public InventoryItem() {
        this.createdAt = LocalDateTime.now();
    }

    public InventoryItem(
            String inventoryName,
            int size,
            String linking,
            String externalNotes,
            InventoryItem parent,
            SoftwareComponent softwareComponent,
            boolean wasCombined,
            boolean curated,
            Project project, String spdxId
    ) {
        this.createdAt = LocalDateTime.now();
        this.inventoryName = inventoryName;
        this.size = size;
        this.linking = linking;
        this.externalNotes = externalNotes;
        this.parent = parent;
        this.softwareComponent = softwareComponent;
        this.wasCombined = wasCombined;
        this.curated = curated;
        this.project = project;
        this.spdxId = spdxId;
    }

    public InventoryItem(String inventoryName, Project project, SoftwareComponent softwareComponent) {
        this.createdAt = LocalDateTime.now();
        this.inventoryName = inventoryName;
        this.project = project;
        this.softwareComponent = softwareComponent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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
