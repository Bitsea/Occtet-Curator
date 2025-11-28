/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.*;

/**
 * Manages the UI state of the AuditView within the user's session.
 * This service stores tab state across page refreshes but not browser restarts.
 */
@Service
@SessionScope
public class AuditViewStateService {

    private static final Logger log = LogManager.getLogger(AuditViewStateService.class);
    private AuditViewState state;

    /**
     * Retrieves the last saved state for the AuditView.
     */
    public Optional<AuditViewState> get() {
        return Optional.ofNullable(state);
    }

    /**
     * Saves the current view state to the session.
     */
    public void save(AuditViewState state) {
        this.state = state;
        log.debug("Saved state to session: project={}, openInventoryTabs={}, openFileTabs={}",
                state.projectId(),
                state.openInventoryTabsIds().size(),
                state.openFileTabsIds().size());
    }

    /**
     * Clears the saved state from the session.
     */
    public void clear() {
        this.state = null;
        log.debug("Cleared session state");
    }

    /**
     * Represents the state of the AuditView, capturing information about the current project,
     * active tabs, expanded nodes, and other session-level UI data. This class is immutable
     * and is used to persist and transfer the UI state within the application.
     */
    public record AuditViewState(
            UUID projectId,
            List<UUID> openInventoryTabsIds,
            List<UUID> openFileTabsIds,
            Serializable activeTabIdentifier,
            Set<UUID> expandedNodeIds
            ) implements Serializable {

        public AuditViewState {
            Objects.requireNonNull(projectId, "Project ID cannot be null");
            openInventoryTabsIds = List.copyOf(openInventoryTabsIds);
            openFileTabsIds = List.copyOf(openFileTabsIds);
            expandedNodeIds = expandedNodeIds != null ? Set.copyOf(expandedNodeIds) : Collections.emptySet();
        }
    }
}