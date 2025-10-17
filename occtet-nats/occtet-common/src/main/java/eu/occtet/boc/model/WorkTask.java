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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * the WorkTask which will be sent to microservices. See BaseWorkData for how to transfer actual data.
 * @param task unique name of the task the microservice should execute
 * @param details (optional) details (only a string)
 * @param timestamp set by the sender
 * @param workData required data for executing the task. Use the appropriate subclass of BaseWorkData or create your own for your task/microservice
 */
@JsonDeserialize
public record WorkTask(String task,String details,long timestamp, BaseWorkData workData) {
}
