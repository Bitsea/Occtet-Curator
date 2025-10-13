package eu.occtet.bocfrontend.view.audit;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import eu.occtet.bocfrontend.dao.CodeLocationRepository;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.model.FileResult;
import eu.occtet.bocfrontend.model.FileTreeNode;
import eu.occtet.bocfrontend.service.FileContentService;
import eu.occtet.bocfrontend.service.FilesTreeService;
import eu.occtet.bocfrontend.view.codeviewerfragment.CodeViewerFragment;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Route(value = "audit-view", layout = MainView.class)
@ViewController(id = "AuditView")
@ViewDescriptor(path = "audit-view.xml")
public class AuditView extends StandardView {

    private static final Logger log = LogManager.getLogger(AuditView.class);

    private static final String SESSION_KEY = "AUDITVIEW_KEY";

    private final Map<InventoryItem, Tab> openInventoryTabs = new HashMap<>();
    private final Map<FileTreeNode, Tab> openFileTabs = new HashMap<>();
    private Map<UUID, Long> fileCounts = new HashMap<>();
    private TreeDataGrid<FileTreeNode> cachedFileTreeGrid;
    private List<FileTreeNode> fileTreeNodes;

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;
    @ViewComponent
    private CollectionContainer<InventoryItem> inventoryItemDc;
    @ViewComponent
    private JmixTabSheet inventoryItemTabSheet;
    @ViewComponent
    private Tab inventoryItemSection;
    @ViewComponent
    private JmixTabSheet filesTabSheet;
    @ViewComponent
    private Tab filesSection;
    @ViewComponent
    private JmixTabSheet mainTabSheet;
    @ViewComponent
    private TreeDataGrid<InventoryItem> inventoryItemDataGrid;
    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private VerticalLayout fileTreeGridLayout;
    @ViewComponent
    private CollectionLoader<InventoryItem> inventoryItemDl;

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
    private FilesTreeService filesTreeService;
    @Autowired
    private FileContentService fileContentService;
    @Autowired
    private CodeLocationRepository codeLocationRepository;
    @Autowired
    private DataManager dataManager;

    @Subscribe
    protected void onInit(InitEvent event) {
        List<Project> projectList = new ArrayList<>(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
        projectComboBox.setItems(projectList);
        inventoryItemDataGrid.setTooltipGenerator(InventoryItem::getInventoryName);
    }


    private void prepareTreeDataGrid() {
        // note: this whole has to be done here and not in the xml because TreeDataGrid is not a persisted object
        // Styling
        if (cachedFileTreeGrid != null) {
            fileTreeGridLayout.removeAll();
            fileTreeGridLayout.add(cachedFileTreeGrid);
            return;
        }

        TreeDataGrid<FileTreeNode> fileTreeGrid = uiComponents.create(TreeDataGrid.class);
        fileTreeGrid.setThemeName("no-row-borders compact row-stripes");
        fileTreeGrid.setWidthFull();
        fileTreeGrid.setHeightFull();

        // Content
        fileTreeGrid.addHierarchyColumn(FileTreeNode::getName).setHeader("File");
        TreeData<FileTreeNode> treeData = new TreeData<>();
        for (FileTreeNode root : fileTreeNodes) {
            treeData.addItem(null, root);
            addChildrenRecursively(treeData, root);
        }

        // Actions
        fileTreeGrid.setTooltipGenerator(FileTreeNode::getName);

        // Double click action
        fileTreeGrid.addItemClickListener(event -> {
            if (event.getClickCount() == 2) {
                FileTreeNode clickedNode = event.getItem();
                if (!clickedNode.isDirectory()) {
                    openFileTabAction(clickedNode);
                }
            }
        });
        // One Click action
        fileTreeGrid.addItemClickListener(event -> {
            FileTreeNode clickedNode = event.getItem();
            if (fileTreeGrid.isExpanded(clickedNode)) {
                fileTreeGrid.collapse(clickedNode);
            } else fileTreeGrid.expand(clickedNode);
        });

        GridContextMenu<FileTreeNode> contextMenu = fileTreeGrid.getContextMenu();
        contextMenu.addItem("Open", event -> {
            openFileTabAction(event.getItem().get());
        });
        contextMenu.addItem("Copy", event -> copyToClipboard(event.getItem().get().getName()));
        contextMenu.addItem("Copy Path", event -> copyToClipboard(filesTreeService.getFullPath(event.getItem().get(),
                event.getItem().get().getName())));
        contextMenu.addItem("Copy Absolute Path", event -> copyToClipboard(event.getItem().get().getFullPath()));
        GridMenuItem<FileTreeNode> openInventoryMenuItem = contextMenu.addItem("Open Inventory", event -> {
            CodeLocation codeLocation = event.getItem().get().getCodeLocation();
            if (codeLocation != null && codeLocation.getInventoryItem() != null) {
                openInventoryItemOpenTabAction(codeLocation.getInventoryItem());
            }
        });

        fileTreeGrid.addComponentColumn(node -> {
           if (node.isDirectory()) {return null;}
           Icon circleIcon = uiComponents.create(Icon.class);
           circleIcon.setIcon(VaadinIcon.CIRCLE);
           circleIcon.setSize("12px");
           String status;
           CodeLocation codeLocation = node.getCodeLocation();
            if (codeLocation == null || codeLocation.getInventoryItem() == null) {
                circleIcon.getStyle().set("color", "var(--lumo-error-color)");
                status = "not in inventory";
            } else if (Boolean.TRUE.equals(codeLocation.getInventoryItem().getCurated())) {
                circleIcon.getStyle().set("color", "var(--lumo-success-color)");
                status = "Curated";
            } else {
                circleIcon.getStyle().set("color", "var(--lumo-primary-color)");
                status = "Included but not curated";
            }

            circleIcon.setTooltipText(status);
            return circleIcon;
        }).setHeader("").setFlexGrow(0).setWidth("20px");

        fileTreeGrid.setDataProvider(new TreeDataProvider<>(treeData));

        this.cachedFileTreeGrid = fileTreeGrid;

        fileTreeGridLayout.removeAll(); // clear old generated grids
        fileTreeGridLayout.add(fileTreeGrid);
    }

    private void addChildrenRecursively(TreeData<FileTreeNode> treeData, FileTreeNode parent) {
        for (FileTreeNode child : parent.getChildren()) {
            treeData.addItem(parent, child);
            if (!child.getChildren().isEmpty()) {
                addChildrenRecursively(treeData, child);
            }
        }
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
        Project selectedProject = event.getValue();
        if (selectedProject == null) {
            inventoryItemDc.setItems(Collections.emptyList());
            fileTreeGridLayout.removeAll();
            return;
        }

        if (event.isFromClient()) {
            refreshInventoryItemDc(event.getValue());

            this.cachedFileTreeGrid = null;
            this.fileTreeNodes = filesTreeService.prepareFilesForTreeGrid(event.getValue());
            prepareTreeDataGrid(); // rebuild

            promptToCloseOldTabs(selectedProject);
        }
    }

    private void promptToCloseOldTabs(Project selectedProject) {
        if (!openInventoryTabs.isEmpty()) {
            dialogs.createOptionDialog()
                    .withHeader("Close tabs")
                    .withText("Do you want to keep old opened tabs for project: " + selectedProject.getProjectName() + " ?")
                    .withActions(
                            new DialogAction(DialogAction.Type.NO)
                                    .withHandler(e -> {
                                        Iterator<Map.Entry<InventoryItem, Tab>> iterator =
                                                openInventoryTabs.entrySet().iterator();
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
        if (event.getClickCount() == 2) {
            // Double-click opens the tab
            openInventoryItemOpenTabAction(event.getItem());
        } else {
            // Single-click expands/collapses
            if (inventoryItemDataGrid.isExpanded(event.getItem())) {
                inventoryItemDataGrid.collapse(event.getItem());
            } else {
                inventoryItemDataGrid.expand(event.getItem());
            }
        }
    }

    public void refreshInventoryItemDc(Project project) {
        inventoryItemDl.setParameter("project", project);
        inventoryItemDl.load();
        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query(
                        "select cl.inventoryItem.id as itemId, count(cl) as fileCount from CodeLocation cl " +
                                "where cl.inventoryItem.project = :project group by cl.inventoryItem.id")
                        .setParameter("project", project))
                .addProperty("itemId")
                .addProperty("fileCount");

        List<KeyValueEntity> counts = dataManager.loadValues(context);

        this.fileCounts = counts.stream()
                .collect(Collectors.toMap(
                        kv -> kv.getValue("itemId"),
                        kv -> (Long) kv.getValue("fileCount")
                ));
    }

    @Supply(to = "inventoryItemDataGrid.fileNumCol", subject = "renderer")
    Renderer<InventoryItem> filesCountRenderer() {
        return new TextRenderer<>(item -> {
            Long count = fileCounts.getOrDefault(item.getId(), 0L);
            return count.toString();
        });
    }

    private void copyToClipboard(String text) {
        UiComponentUtils.copyToClipboard(text)
                .then(successResult -> notifications.create("Text copied!")
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show(),
                        errorResult -> notifications.create("Copy failed!")
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                                .show());
    }

    private <T> void openTabAction(Map<T, Tab> openTabsMap, JmixTabSheet tabSheet, T key, String tabTitle,
                                   Supplier<Component> contentSupplier, Consumer<Tab> onClose) {
        if (openTabsMap.containsKey(key)) {
            tabSheet.setSelectedTab(openTabsMap.get(key));
            return;
        }

        Component fragment = contentSupplier.get();
        Tab tab = tabSheet.add(tabTitle, fragment);

        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(click -> {
            if (onClose != null)
                onClose.accept(tab);
        });

        tab.addComponentAtIndex(1, closeButton);
        openTabsMap.put(key, tab);
        tabSheet.setSelectedTab(tab);

        saveStateToSession();
    }
    /**
     * Opens an inventory item in a new tab within the inventory item tab sheet.
     * If the tab for the selected inventory item is already open, it focuses on the existing tab.
     * Otherwise, it creates a new tab for the selected inventory item, adds a close button to it,
     * and saves the current state to the session.
     */
    private void openInventoryItemOpenTabAction(InventoryItem selected) {
        mainTabSheet.setSelectedTab(inventoryItemSection);

        openTabAction(openInventoryTabs, inventoryItemTabSheet, selected, selected.getInventoryName(),
               () -> {
                   InventoryItemTabFragment fragment = fragments.create(this, InventoryItemTabFragment.class);
                   fragment.setHostView(this);
                   fragment.setInventoryItem(selected);
                   return fragment;
               },
               tab -> {
                   handleClosingInventoryItemTab(selected, tab);}
        );
    }

    private void openFileTabAction(FileTreeNode file) {
        mainTabSheet.setSelectedTab(filesSection);

        openTabAction(openFileTabs, filesTabSheet, file, file.getName(),
                () -> {
                    CodeViewerFragment fragment = fragments.create(this, CodeViewerFragment.class);
                    FileResult res = fileContentService.getFileContent(file.getFullPath());
                    if (res instanceof FileResult.Success(String content, String fileName)) {
                        fragment.setCodeEditorContent(content, fileName);
                    } else {
                        if (res instanceof FileResult.Failure(String errorMessage)) {
                            log.warn("Could not view code location: {}", errorMessage);
                            notifications.create(errorMessage)
                                    .withPosition(Notification.Position.BOTTOM_END)
                                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                                    .withDuration(6000)
                                    .show();
                        }
                    }
                    return fragment;
                }, tab -> {
                    closeTab(file, tab);
                });
    }

    /**
     * Handles the closing of an inventory item tab in the inventory item tab sheet.
     * If there are unsaved changes in the associated inventory item, the user is prompted
     * to either discard the changes or keep the tab open. Otherwise, the tab is closed
     * and the session state is updated.
     */
    private void handleClosingInventoryItemTab(InventoryItem selected, Tab tab) {
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

    private <T> void closeTab(T selected, Tab tab){
        inventoryItemTabSheet.remove(tab);
        openInventoryTabs.remove(selected);
        saveStateToSession();
    }

    /**
     * Saves the current state of the inventory
     */
    private void saveStateToSession() {
        Project selectedProject = projectComboBox.getValue();

        List<UUID> openInventoryItemTabsIds = openInventoryTabs.keySet().stream()
                .map(InventoryItem::getId)
                .toList();

        List<FileTreeNode> openFileTabPaths = new ArrayList<>(openFileTabs.keySet());

        AuditViewState state = new AuditViewState(
                selectedProject != null ? selectedProject.getId() : null,
                openInventoryItemTabsIds,
                openFileTabPaths,
                this.fileTreeNodes
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

        if (state.openInventoryTabsIds() != null) {
            state.openInventoryTabsIds().forEach(id ->
                    inventoryItemRepository.findById(id).ifPresent(this::openInventoryItemOpenTabAction));
        }

        if (state.openFileTabsPaths() != null) {
            state.openFileTabsPaths().forEach(this::openFileTabAction);
        }

        if (state.fileTreeNodes() != null && !state.fileTreeNodes().isEmpty()) {
            this.fileTreeNodes = state.fileTreeNodes();
            prepareTreeDataGrid();
        }
    }

    // DTO for storing session state
    public record AuditViewState(
            UUID projectId,
            List<UUID> openInventoryTabsIds,
            List<FileTreeNode> openFileTabsPaths,
            List<FileTreeNode> fileTreeNodes
    ) implements Serializable {
        @Override
        public String toString() {
            return "AuditViewState{" +
                    "projectId=" + projectId +
                    ", openInventoryTabsIds=" + openInventoryTabsIds +
                    ", openFileTabsPaths=" + openFileTabsPaths +
                    ", fileTreeNodes=" + fileTreeNodes +
                    '}';
        }
    }
}