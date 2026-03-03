/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */


package eu.occtet.bocfrontend.entity;



import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

@JmixEntity
@Table(name = "ORT_ISSUE")
@Entity
public class OrtIssue {


    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @OnDelete(DeletePolicy.CASCADE)
    private Project project;

    @Column(name= "MESSAGE",columnDefinition = "TEXT")
    private String message;

    @Column(name="RESOLVED")
    private Boolean resolved;

    @Column(name="RESOLUTION", columnDefinition = "TEXT")
    private String resolution;

    @Column(name="AFFECTED_PATH", columnDefinition = "TEXT")
    private String affectedPath;

    @Column(name="SEVERITY")
    private String severity;

    @Column(name="SOURCE")
    private String source;

    @Column(name="PURL")
    private String purl;

    @Column(name="WORKER")
    private String worker;

    @Column(name="TIMESTAMP")
    private String timestamp;

    @Column(name="IDENTIFIER")
    private String identifier;

    @ManyToOne
    @JoinColumn(name="INVENTORY_ITEM_ID")
    private InventoryItem inventoryItem;


    public OrtIssue() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAffectedPath() {
        return affectedPath;
    }

    public void setAffectedPath(String affectedPath) {
        this.affectedPath = affectedPath;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
}
