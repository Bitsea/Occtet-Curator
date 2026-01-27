package eu.occtet.bocfrontend.util;

import com.vaadin.flow.component.icon.Icon;
import eu.occtet.bocfrontend.entity.CuratorTask;
import io.jmix.flowui.kit.component.ComponentUtils;

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
public class CuratorTaskUI {
    public static Icon iconForTask(CuratorTask task) {

        switch(task.getStatus()) {
            case CREATING: {
                Icon icon = ComponentUtils.parseIcon("vaadin:clock");
                icon.setColor("orange");
                return icon;
            }
            case APPROVE:
            {
                Icon icon = ComponentUtils.parseIcon("vaadin:flag");
                icon.setColor("orange");
                return icon;
            }
            case CANCELLED:
            {
                Icon icon = ComponentUtils.parseIcon("vaadin:close");
                icon.setColor("red");
                return icon;
            }
            case IN_PROGRESS: {
                Icon icon = ComponentUtils.parseIcon("vaadin:clock");
                icon.setColor("green");
                return icon;
            }
            case COMPLETED: {
                Icon icon = ComponentUtils.parseIcon("vaadin:check");
                icon.setColor("green");
                return icon;
            }

            default:
                return ComponentUtils.parseIcon("vaadin:question");
        }



    }
}
