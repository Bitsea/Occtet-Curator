/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.entity.spdxV2;

import jakarta.persistence.*;

@Embeddable
public class Pointer {

    @Column(nullable = false)
    private String reference;

    private Integer offset;

    private Integer lineNumber;

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getOffset() {
        return offset;
    }

    public String getReference() {
        return reference;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
