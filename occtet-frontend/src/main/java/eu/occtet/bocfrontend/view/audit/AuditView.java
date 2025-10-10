package eu.occtet.bocfrontend.view.audit;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoIcon;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.service.InventoryItemService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.*;
import java.util.List;

@Route(value = "audit-view", layout = MainView.class)
@ViewController(id = "AuditView")
@ViewDescriptor(path = "audit-view.xml")
public class AuditView extends StandardView {

    private static final Logger log = LogManager.getLogger(AuditView.class);

    private static final String SESSION_KEY = "AUDITVIEW_KEY";

    private final Map<InventoryItem, Tab> openTabs = new HashMap<>();

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;
    @ViewComponent
    private CollectionContainer<InventoryItem> inventoryItemDc;
    @ViewComponent
    private JmixTabSheet inventoryItemTabSheet;
    @ViewComponent
    private TreeDataGrid<InventoryItem> inventoryItemDataGrid;
    @ViewComponent
    private DataContext dataContext;

    @Autowired
    private Fragments fragments;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Notifications notifications;
    @Autowired
    private InventoryItemService inventoryItemService;

    @Subscribe
    protected void onInit(InitEvent event) {
        List<Project> projectList = new ArrayList<>(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
        projectComboBox.setItems(projectList);
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        restoreStateFromSession();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        saveStateToSession();

        // TODO handle unsaved changes if view is closed without saving
    }

    /**
     * Handles the value change event for the projectComboBox component.
     * This method updates the inventory items associated with the selected project
     * and prompts the user to decide whether to close or keep any open tabs related to the previous project.
     */
    @Subscribe("projectComboBox")
    public void onProjectFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (event.getValue() != null) {
            refreshInventoryItemDc(event.getValue());
        }

        if (!openTabs.isEmpty()) {
            dialogs.createOptionDialog()
                    .withHeader("Close tabs")
                    .withText("Do you want to keep old opened tabs for project: " + event.getValue().getProjectName() + " ?")
                    .withActions(
                            new DialogAction(DialogAction.Type.NO)
                                    .withHandler(e -> {
                                        Iterator<Map.Entry<InventoryItem, Tab>> iterator = openTabs.entrySet().iterator();
                                        while (iterator.hasNext()) {
                                            Map.Entry<InventoryItem, Tab> entry = iterator.next();
                                            inventoryItemTabSheet.remove(entry.getValue());
                                            iterator.remove();
                                        }
                                        saveStateToSession();
                                    }),
                            new DialogAction(DialogAction.Type.YES).withText("Keep")
                    )
                    .open();
        }
    }

    /**
     * Handles the click event on an inventory item in the data grid.
     * Expands or collapses the clicked inventory item row based on its current state.
     */
    @Subscribe("inventoryItemDataGrid")
    public void onInventoryItemDataGridClick(final ItemClickEvent<InventoryItem> event) {
        if (inventoryItemDataGrid.isExpanded(event.getItem())) {
            inventoryItemDataGrid.collapse(event.getItem());
        } else {
            inventoryItemDataGrid.expand(event.getItem());
        }
    }

    public void refreshInventoryItemDc(Project project) {
        List<InventoryItem> inventoryItems = inventoryItemRepository.findByProject(project);
        inventoryItemDc.setItems(inventoryItems);
    }

    /**
     * Provides a renderer that generates a button component for each inventory item in the data grid.
     * The button allows users to open a specific inventory item in a new tab for viewing or editing purposes.
     */
    @Supply(to = "inventoryItemDataGrid.viewActionColumn", subject = "renderer")
    protected Renderer<InventoryItem> inventoryNameRenderer() {
        return new ComponentRenderer<>(inventoryItem -> {
            JmixButton viewButton = uiComponents.create(JmixButton.class);
            viewButton.setIcon(LumoIcon.EDIT.create());
            viewButton.addClickListener(click -> openInventoryItemOpenTabAction(inventoryItem));
            viewButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            viewButton.setTooltipText("view: " + inventoryItem.getInventoryName());
            viewButton.setWidth("25px");
            viewButton.setHeight("25px");
            return viewButton;
        });
    }

    /**
     * Opens an inventory item in a new tab within the inventory item tab sheet.
     * If the tab for the selected inventory item is already open, it focuses on the existing tab.
     * Otherwise, it creates a new tab for the selected inventory item, adds a close button to it,
     * and saves the current state to the session.
     */
    private void openInventoryItemOpenTabAction(InventoryItem selected) {
        if (openTabs.containsKey(selected)) {
            inventoryItemTabSheet.setSelectedTab(openTabs.get(selected));
            return;
        }

        InventoryItemTabFragment fragment = fragments.create(this, InventoryItemTabFragment.class);
        fragment.setHostView(this);
        fragment.setInventoryItem(selected);
        Tab tab = inventoryItemTabSheet.add(selected.getInventoryName(), fragment);

        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
        closeButton.addClickListener(click -> handleClosingTab(selected, tab));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        tab.addComponentAtIndex(1, closeButton);

        openTabs.put(selected, tab);
        inventoryItemTabSheet.setSelectedTab(tab);

        saveStateToSession();
    }

    /**
     * Handles the closing of an inventory item tab in the inventory item tab sheet.
     * If there are unsaved changes in the associated inventory item, the user is prompted
     * to either discard the changes or keep the tab open. Otherwise, the tab is closed
     * and the session state is updated.
     */
    private void handleClosingTab(InventoryItem selected, Tab tab) {
        InventoryItemTabFragment fragment = (InventoryItemTabFragment) inventoryItemTabSheet.getComponent(tab);
        if (fragment != null && dataContext.isModified(fragment.getInventoryItem())){
            notifications.create("Unsaved changes detected")
                    .withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                    .show();
            dialogs.createOptionDialog()
                    .withHeader("Unsaved changes detected")
                    .withText("Want to discard changes?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES)
                                    .withHandler(e -> {closeTab(selected, tab);}),
                            new DialogAction(DialogAction.Type.NO)
                    ).open();
        } else {
            closeTab(selected, tab);
        }
        saveStateToSession();
    }

    private void closeTab(InventoryItem selected, Tab tab){
        inventoryItemTabSheet.remove(tab);
        openTabs.remove(selected);
        saveStateToSession();
    }


    /**
     * Saves the current state of the inventory
     */
    private void saveStateToSession() {
        Project selectedProject = projectComboBox.getValue();
        List<UUID> openTabsIds = openTabs.keySet().stream()
                .map(InventoryItem::getId)
                .toList();

        AuditViewState state = new AuditViewState(
                selectedProject != null ? selectedProject.getId() : null,
                openTabsIds
        );

        try {
            VaadinSession.getCurrent().setAttribute(SESSION_KEY, state);
            log.debug("Saved state to session: {}", state);
        } catch (Exception e) {
            log.error("Error saving state to session", e);
        }
    }

    /**
     * Restores the state of the inventory from the session.
     */
    private void restoreStateFromSession() {
        Object stateObject = VaadinSession.getCurrent().getAttribute(SESSION_KEY);

        if (!(stateObject instanceof AuditViewState state)) {
            log.debug("No valid session state found for AuditView");
            return;
        }

        log.debug("Restoring state from session: {}", state);

        if (state.projectId() != null) {
            projectRepository.findById(state.projectId())
                    .ifPresent(projectComboBox::setValue);
        }

        if (state.openTabsIds() != null && !state.openTabsIds().isEmpty()) {
            List<InventoryItem> itemsToOpen = new ArrayList<>();
            state.openTabsIds().forEach(id -> itemsToOpen.add(inventoryItemRepository.findById(id).orElse(null)));
            itemsToOpen.forEach(this::openInventoryItemOpenTabAction);
        }
    }

    // DTO for storing session state
    public record AuditViewState(UUID projectId, List<UUID> openTabsIds) implements Serializable {

    @Override
        public String toString() {
            return "AuditViewState{" +
                    "projectId=" + projectId +
                    ", openTabsIds=" + openTabsIds +
                    '}';
        }
    }
}