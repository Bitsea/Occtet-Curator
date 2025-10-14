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

package eu.occtet.bocfrontend.validator;

import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.exception.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PathValidator implements Validator<String> {

    private static final Logger log = LogManager.getLogger(PathValidator.class);

    @Override
    public void accept(String value) {
        log.debug("PathValidator called with value: {}", value);

        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Path cannot be empty.");
        }

        try {
            Path path = Paths.get(value);
            if (path.isAbsolute()) {
                throw new ValidationException("Path must be relative.");
            }
        } catch (InvalidPathException e) {
            throw new ValidationException("Invalid path format. Please enter a valid relative path.", e);
        }
    }
}
