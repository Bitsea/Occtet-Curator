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



@Entity
@Table(name = "CONFIGURATION")
@EntityListeners(AuditingEntityListener.class)
public class Configuration {

    public enum Type {STRING, NUMERIC, FILE_UPLOAD, BASE_PATH, COMMA_SEPARATED_STRINGS, BOOLEAN}

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "UPLOAD", length =1000000000)
    private byte[] upload;

    public Configuration() {
    }

    public Configuration(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Long getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte[] getUpload() {
        return upload;
    }

    public void setUpload(byte[] upload) {
        this.upload = upload;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
