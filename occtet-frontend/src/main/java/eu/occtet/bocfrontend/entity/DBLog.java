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
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;




@JmixEntity
@Table(name = "DBLOG")
@Entity
public class DBLog {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    /**
     * Audit-of-creation timestamp.
     * Automatically set by the framework when the entity is first saved.
     */
    @CreatedDate
    @Column(name = "EVENT_DATE", nullable = false, updatable = false)
    private LocalDateTime eventDate;

    @Column(name = "COMPONENT", length = 20)
    private String component;

    /**
     * Makes the log message the “instance name” visible in the UI.
     */
    @InstanceName
    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    public DBLog() {
    }

    public DBLog(String component, String message) {
        this.component = component;
        this.message = message;
    }

    // ——— Getters & setters ———

    public Long getId() {
        return id;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public String getComponent() {
        return component;
    }
    public void setComponent(String component) {
        this.component = component;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }
}