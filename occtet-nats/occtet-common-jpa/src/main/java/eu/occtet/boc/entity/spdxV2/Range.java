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
public class Range {
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "reference", column = @Column(name = "start_reference", nullable = false)),
            @AttributeOverride(name = "offset", column = @Column(name = "start_offset")),
            @AttributeOverride(name = "lineNumber", column = @Column(name = "start_line_number"))
    })
    private Pointer startPointer;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "reference", column = @Column(name = "end_reference", nullable = false)),
            @AttributeOverride(name = "offset", column = @Column(name = "end_offset")),
            @AttributeOverride(name = "lineNumber", column = @Column(name = "end_line_number"))
    })
    private Pointer endPointer;

    public Pointer getEndPointer() {
        return endPointer;
    }

    public Pointer getStartPointer() {
        return startPointer;
    }

    public void setEndPointer(Pointer endPointer) {
        this.endPointer = endPointer;
    }

    public void setStartPointer(Pointer startPointer) {
        this.startPointer = startPointer;
    }
}
