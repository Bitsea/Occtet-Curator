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

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "CODE_LOCATION")
@EntityListeners(AuditingEntityListener.class)
public class CodeLocation {

    @Id
    @Column(name="ID", nullable = false, columnDefinition = "UUID")
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "INVENTORY_ITEM_ID", columnDefinition = "UUID")
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