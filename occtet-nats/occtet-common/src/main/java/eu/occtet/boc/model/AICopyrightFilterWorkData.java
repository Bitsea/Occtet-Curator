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

package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.List;


public class AICopyrightFilterWorkData extends BaseWorkData {

    private String userMessage;
    private Long inventoryItemId;
    private List<String> questionableCopyrights;


    public AICopyrightFilterWorkData( Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    @JsonCreator
    public AICopyrightFilterWorkData(@JsonProperty("userMessage")String userMessage,
                                     @JsonProperty("inventoryItemId")Long inventoryItemId,
                                     @JsonProperty("questionableCopyrights")List<String> questionableCopyrights) {
        this.userMessage = userMessage;
        this.inventoryItemId = inventoryItemId;
        this.questionableCopyrights= questionableCopyrights;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public List<String> getQuestionableCopyrights() {
        return questionableCopyrights;
    }

    public void setQuestionableCopyrights(List<String> questionableCopyrights) {
        this.questionableCopyrights = questionableCopyrights;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }

}
