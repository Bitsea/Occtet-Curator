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

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;

@JmixEntity
@Table(name = "CODE_LOCATION")
@Entity
public class CodeLocation {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "INVENTORY_ITEM_ID")
    private InventoryItem inventoryItem;

    @Column(name = "FILE_PATH", columnDefinition = "Text")
    private String filePath;

    @Column(name= "LINE_NUMBER")
    private Integer lineNumber;

    @Column(name= "LINE_NUMBER_TO")
    private Integer lineNumberTo;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name= "CODE_LOCATION_ID")
    private List<Copyright> copyrights;

    public CodeLocation(){}

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getLineNumberTo() {
        return lineNumberTo;
    }

    public void setLineNumberTo(Integer lineNumberTo) {
        this.lineNumberTo = lineNumberTo;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public List<Copyright> getCopyrights() {return copyrights;}

    public void setCopyrights(List<Copyright> copyrights) {this.copyrights = copyrights;}
}