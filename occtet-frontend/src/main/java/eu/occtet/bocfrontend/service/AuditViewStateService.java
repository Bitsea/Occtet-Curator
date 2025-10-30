package eu.occtet.bocfrontend.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
                state.openFileTabsPaths().size());
    }

    /**
     * Clears the saved state from the session.
     */
    public void clear() {
        this.state = null;
        log.debug("Cleared session state");
    }

    /**
     * A data record to hold the serializable state of the AuditView.
     *
     * @param projectId            The ID of the currently selected project
     * @param openInventoryTabsIds List of UUIDs for all open inventory item tabs
     * @param openFileTabsPaths    List of file paths (Strings) for all open file tabs
     * @param activeTabIdentifier  Identifier for the currently focused tab (UUID for inventory, String path for file)
     */
    public record AuditViewState(
            UUID projectId,
            List<UUID> openInventoryTabsIds,
            List<String> openFileTabsPaths,
            Serializable activeTabIdentifier
    ) implements Serializable {

        /**
         * Compact constructor with validation
         */
        public AuditViewState {
            Objects.requireNonNull(projectId, "Project ID cannot be null");
            openInventoryTabsIds = List.copyOf(openInventoryTabsIds);
            openFileTabsPaths = List.copyOf(openFileTabsPaths);
        }
    }
}