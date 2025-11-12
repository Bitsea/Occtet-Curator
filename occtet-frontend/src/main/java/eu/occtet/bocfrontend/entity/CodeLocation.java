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