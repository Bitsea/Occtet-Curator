/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "FILE", indexes = {
        @Index(name = "IDX_FILE_PROJECT_PARENT", columnList = "PROJECT_ID, PARENT_ID"),
        @Index(name = "IDX_FILE_PARENT", columnList = "PARENT_ID"),
        @Index(name = "IDX_FILE_PROJECT_REVIEWED", columnList = "PROJECT_ID, REVIEWED")
})
@EntityListeners(AuditingEntityListener.class)
public class File {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "PARENT_ID")
    private File parent;


    @ManyToMany(mappedBy = "files", cascade = CascadeType.REMOVE)
    private Set<Copyright> copyrights = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVENTORY_ITEM_ID")
    private InventoryItem inventoryItem;

    @Column(name = "FILENAME")
    private String fileName;

    // Example: C:\Users\Temp\scan\project\dep\lib\com\acme\Util.java
    @Column(name = "PHYSICAL_PATH", columnDefinition = "TEXT")
    private String physicalPath;

    // Example: project_101/dependencies/lib-v1/com/acme/Util.java
    @Column(name = "PROJECT_PATH", nullable = false, columnDefinition = "TEXT")
    private String projectPath;

    // Example: com/acme/Util.java
    @Column(name = "ARTIFACT_PATH", columnDefinition = "TEXT")
    private String artifactPath;

    @Column(name = "IS_DIRECTORY")
    private Boolean isDirectory = false;

    @Column(name = "REVIEWED")
    private Boolean reviewed = false;

    @CreatedDate
    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "LAST_MODIFIED_BY")
    private String lastModifiedBy;


    public File() {
    }

    public File( String filePath, Project project){
        this.projectPath= filePath;
        this.project= project;
    }

    public File ( InventoryItem inventoryItem, String filePath, String fileName, Project project){
        this.inventoryItem= inventoryItem;
        this.projectPath= filePath;
        this.fileName= fileName;
        this.project= project;
    }

    public File ( InventoryItem inventoryItem, String filePath, Project project) {
        this.inventoryItem = inventoryItem;
        this.projectPath = filePath;
        this.project= project;
    }

    public File ( String artifactPath, Project project, String fileName, InventoryItem inventoryItem) {
        this.artifactPath= artifactPath;
        this.fileName = fileName;
        this.project= project;
        this.inventoryItem= inventoryItem;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
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

    public File getParent() {
        return parent;
    }

    public void setParent(File parent) {
        this.parent = parent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Boolean directory) {
        isDirectory = directory;
    }

    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Boolean getDirectory() {
        return isDirectory;
    }

    public void setDirectory(Boolean directory) {
        isDirectory = directory;
    }

    public String getPhysicalPath() {
        return physicalPath;
    }

    public void setPhysicalPath(String physicalPath) {
        this.physicalPath = physicalPath;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        File file = (File) o;
        return Objects.equals(id, file.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "File{" + "id=" + id + ", fileName='" + fileName + '\'' + ", isDirectory=" + isDirectory + '}';
    }

    public Set<Copyright> getCopyrights() {
        return copyrights;
    }

    public void setCopyrights(Set<Copyright> copyrights) {
        this.copyrights = copyrights;
    }

    @PreRemove
    private void removeBookAssociations() {
        for (Copyright copyright: this.copyrights) {
            copyright.getFiles().remove(this);
        }
    }


}
