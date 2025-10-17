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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "DBLOG")
@EntityListeners(AuditingEntityListener.class)
public class DBLog {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    // Should automatically set the current timestamp on creation
    @Column(name = "EVENT_DATE", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime eventDate;

    @Column(name = "COMPONENT", length = 20)
    private String component;

    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    // Constructors
    public DBLog() {
    }

    public DBLog(String component, String message) {
        this.component = component;
        this.message = message;
    }

    // Getters and Setters

    public UUID getId() {
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
}
