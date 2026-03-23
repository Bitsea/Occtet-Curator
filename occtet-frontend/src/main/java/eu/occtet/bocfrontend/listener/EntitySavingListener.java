/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.listener;

import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.service.UserService;
import io.jmix.core.Messages;
import io.jmix.core.event.EntitySavingEvent;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.exception.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures organizational data integrity during the creation of new tenant-specific entities.
 * <p>
 * This listener intercepts the save lifecycle for any entity implementing {@link HasOrganization}.
 * If an entity is newly instantiated and lacks an assigned organization, it automatically inherits
 * the organization associated with the currently authenticated user. This enforces strict data
 * isolation while preventing manual assignment errors in the UI controllers.
 */
@Component
public class EntitySavingListener {

    private static final Logger log = LogManager.getLogger(EntitySavingListener.class);

    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private UserService userService;
    @Autowired
    private Messages messages;

    /**
     * Assigns the current user's organization to the target entity before it is persisted to the database.
     * <p>
     * This method evaluates the entity's state to prevent overriding an explicitly defined
     * organization (e.g., when a system administrator creates a record on behalf of a different tenant).
     * It gracefully ignores unauthenticated contexts, system users, or users without an assigned organization.
     *
     * @param event the lifecycle event containing the entity being saved
     */
    @EventListener
    public void onEntitySaving(final EntitySavingEvent<?> event) {

        if (!event.isNewEntity()) {
            return;
        }

        Object rawEntity = event.getEntity();

        if (rawEntity instanceof HasOrganization entity) {

            if (entity.getOrganization() == null) {

                log.debug("Auth set: {}, user type: {}",
                        currentAuthentication.isSet(),
                        currentAuthentication.isSet() ? currentAuthentication.getUser().getClass().getSimpleName() : "N/A");

                if (currentAuthentication.isSet() && currentAuthentication.getUser() instanceof User currentUser) {

                    if (currentUser.getOrganization() != null) {
                        log.info("Setting organization '{}' for new entity '{}'",
                                currentUser.getOrganization().getOrganizationName(),
                                entity.getClass().getSimpleName());
                        entity.setOrganization(currentUser.getOrganization());

                    } else if (!userService.isAdmin()){
                        throw new ValidationException(
                                messages.getMessage("orgAssignmentError")
                        );
                    } else {
                        throw new ValidationException(
                                messages.getMessage("orgAssignmentErrorAdmin")
                        );
                    }
                }
            }
        }
    }
}
