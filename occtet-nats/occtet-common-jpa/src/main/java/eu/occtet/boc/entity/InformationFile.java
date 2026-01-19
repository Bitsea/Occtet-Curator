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



@Entity
@Table(name="INFORMATION_FILE",uniqueConstraints = { @UniqueConstraint(columnNames = { "FILE_NAME"}) })
public class InformationFile {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @Column(name= "FILE_NAME")
    private String fileName;

    @Column(name= "FILE_PATH",columnDefinition = "TEXT")
    private String filePath;

    @Column(name="FILE_CONTEXT",columnDefinition = "TEXT")
    private String context;

    @Column(name="FILEINFORMATION_CONTENT",columnDefinition = "TEXT")
    private String content;

    public InformationFile(){}

    public InformationFile(String filename, String context, String content, String path) {
        this.fileName = filename;
        this.context = context;
        this.content = content;
        this.filePath= path;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
