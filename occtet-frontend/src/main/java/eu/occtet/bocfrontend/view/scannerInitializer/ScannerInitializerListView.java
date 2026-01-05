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

package eu.occtet.bocfrontend.view.scannerInitializer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;


import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.occtet.bocfrontend.dao.ScannerInitializerRepository;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;

import eu.occtet.bocfrontend.engine.ScannerManager;
import eu.occtet.bocfrontend.service.ScannerInitializerService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.action.list.CreateAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.facet.Timer;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Route(value = "scannerInitializer", layout = MainView.class)
@ViewController("ScannerInitializer.list")
@ViewDescriptor("scannerInitializer-list-view.xml")
@LookupComponent("scannerInitializerDataGrid")
@DialogMode(width = "600", height = "800")
public class ScannerInitializerListView extends StandardListView<ScannerInitializer> {


    private static final Logger log = LogManager.getLogger(ScannerInitializerListView.class);

    @ViewComponent
    private NativeLabel waiting;
    @ViewComponent
    private NativeLabel stopped;
    @ViewComponent
    private NativeLabel completed;
    @ViewComponent
    private NativeLabel inProgress;
    @ViewComponent
    private DataGrid<ScannerInitializer> scannerInitializerDataGrid;
    @ViewComponent
    private JmixButton showFeedback;
    @ViewComponent
    private JmixButton showConfig;
    @ViewComponent
    private JmixButton stopScannerBtn;
    @ViewComponent
    private HorizontalLayout availableScannerBox;
    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent("scannerInitializerDataGrid.create")
    private CreateAction<ScannerInitializer> scannerInitializersDataGridCreate;
    @ViewComponent
    private CollectionContainer<ScannerInitializer> scannerInitializerDc;
    @ViewComponent
    private HorizontalLayout runningScannerBox;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Dialogs messageDialog;
    @Autowired
    private ScannerManager scannerManager;
    @Autowired
    private ScannerInitializerService scannerInitializerService;
    @Autowired
    private DataManager dataManager;
    @ViewComponent
    private Timer progressTimer;
    private double runningScannerProgress = 0;


    private static VerticalLayout runningVBoxLayout;
    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        scannerManager.setTaskListView(this);
        updateStatsPanel();
        updateAvailableScannersBox();
        updateScannerQueueBox();
        scannerInitializerDc.setItems(scannerInitializerRepository.findByStatus(ScannerInitializerStatus.COMPLETED.getId()));

    }

    @Subscribe("progressTimer")
    protected void onTimerTick(Timer.TimerActionEvent event) {
        if (runningVBoxLayout != null && runningVBoxLayout.getComponentAt(1) != null &&
                runningVBoxLayout.getChildren().count() > 1 &&
                runningVBoxLayout.getComponentAt(1) instanceof ProgressBar) {
            ProgressBar progressBar = (ProgressBar) runningVBoxLayout.getComponentAt(1);
            progressBar.setValue(runningScannerProgress);
        }
    }

    /**
     * loads preexisting tasks to the grid
     */
    private void load(){
        List<ScannerInitializer> scannerInitializers = new ArrayList<>();
        scannerInitializers.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.COMPLETED));
        scannerInitializers.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.WAITING));
        scannerInitializers.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.IN_PROGRESS));
        scannerInitializers.addAll(scannerInitializerService.getScannerByStatus(ScannerInitializerStatus.STOPPED));
        scannerInitializerDc.setItems(scannerInitializers);
    }

    private void updateStatsPanel() {
        stopped.setText("stopped: " + scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.STOPPED));
        waiting.setText("waiting: " + scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.WAITING));
        inProgress.setText("in progress: " + scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.IN_PROGRESS));
        completed.setText("completed: " + scannerInitializerService.countScannerByStatus(ScannerInitializerStatus.COMPLETED));
    }

    private void updateAvailableScannersBox() {
        List<String> scanners = scannerManager.getAvailableScanners();
        availableScannerBox.removeAll();
        scanners.forEach(scanner -> {
            if(!scanner.contains("Dumb") && !scanner.contains("Flexera_Report_Scanner"))
                availableScannerBox.add(createScannerIcon(scanner + "Id", scanner, false));
        });
    }

    private void updateScannerQueueBox() {
        runningScannerBox.removeAll();
        List<ScannerInitializer> scannerInitializers = scannerManager.getWaitingAndRunningScanners();
        scannerInitializers.stream().forEach(scanner -> runningScannerBox.add(CreateWidgets(scanner)));
        if(scannerInitializers.size()==0)
            progressTimer.stop();
        else progressTimer.start();
    }


    private VerticalLayout createScannerIcon(String buttonId, String scanner,boolean isRunning) {
        VerticalLayout verticalLayout = uiComponents.create(VerticalLayout.class);
        verticalLayout.setPadding(false);
        verticalLayout.setWidth("AUTO");
        verticalLayout.addClassName(LumoUtility.AlignItems.CENTER);
        verticalLayout.addClassName(LumoUtility.JustifyContent.CENTER);

        H6 h6 = uiComponents.create(H6.class);
        h6.setText(scanner);

        JmixImage<Object> scannerImageButton = uiComponents.create(JmixImage.class);
        scannerImageButton.setClassName("image-border");
        scannerImageButton.setId(buttonId);
        scannerImageButton.setHeight("100px");
        scannerImageButton.setWidth("100px");
        scannerImageButton.getElement().setAttribute("src", "icons/" + scanner.replace(" ", "") + ".png");
        verticalLayout.add(scannerImageButton, h6);

        if (!isRunning) {
            scannerImageButton.setTitle(messageBundle.getMessage(scanner.replace(" ", "")));
            scannerImageButton.addClickListener(e -> {
                scannerManager.preselectNewScanner(scanner);
                scannerInitializersDataGridCreate.execute();
            });
        } else {
            scannerImageButton.setTitle(messageBundle.getMessage("scanning"));
        }
        return verticalLayout;
    }

    public VerticalLayout CreateWidgets(ScannerInitializer scannerInitializer) {
        VerticalLayout vBoxLayout = uiComponents.create(VerticalLayout.class);
        vBoxLayout.setWidth("AUTO");
        vBoxLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        ProgressBar progressBar = uiComponents.create(ProgressBar.class);
        Button btn= uiComponents.create(Button.class);
        btn.setText("btn");

        if (runningScannerProgress >= 0 && ScannerInitializerStatus.IN_PROGRESS.getId().equals(scannerInitializer.getStatus()))
            progressBar.setValue(runningScannerProgress);
        VerticalLayout iconButton = createScannerIcon(scannerInitializer.getScanner() + "RunningId", scannerInitializer.getScanner(), true);
        vBoxLayout.add(iconButton);
        vBoxLayout.add(progressBar);
        progressBar.setWidthFull();

        if (ScannerInitializerStatus.IN_PROGRESS.getId().equals(scannerInitializer.getStatus()))
            ScannerInitializerListView.runningVBoxLayout = vBoxLayout;
        return vBoxLayout;
    }

    @Subscribe("refreshTimer")
    public void onRefreshTimerTimerAction(final Timer.TimerActionEvent event) {
        updateStatsPanel();
        updateScannerQueueBox();
        scannerInitializerDc.setItems(scannerInitializerRepository.findByStatus(ScannerInitializerStatus.COMPLETED.getId()));
    }

    @Subscribe("scannerInitializerDataGrid")
    public void onScannerInitializerDataGridItemClick(final ItemClickEvent<ScannerInitializer> event) {
        showFeedback.setEnabled(false);
        showConfig.setEnabled(false);
        if (event.getItem() != null) {
            showFeedback.setEnabled(event.getItem().getStatus().equals(ScannerInitializerStatus.COMPLETED.name()));
            showConfig.setEnabled(!event.getItem().getScannerConfiguration().isEmpty());
        }
    }

    @Subscribe(id = "showConfig", subject = "clickListener")
    public void onShowConfigClick1(final ClickEvent<JmixButton> event) {
        Set<ScannerInitializer> selectedItems = scannerInitializerDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        List<Configuration> configList = selectedItems.iterator().next().getScannerConfiguration();
        sb.append("\\<div>");
        for (Configuration cf : configList) {
            sb.append("<b>" + cf.getName() + ":</b> " + cf.getValue() + "<p>");
        }
        sb.append("</div>");
        messageDialog.createMessageDialog()
                .withHeader("Configuration")
                .withContent(new Html(sb.toString()))
                .open();
    }

    @Subscribe(id = "stopScannerBtn", subject = "clickListener")
    public void onStopClick(final ClickEvent<JmixButton> event) {
        Set<ScannerInitializer> selectedItems = scannerInitializerDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        for (ScannerInitializer scannerInitializer : selectedItems) {
            scannerInitializer.updateStatus(ScannerInitializerStatus.STOPPED.getId());
            dataManager.save(scannerInitializer);
        }
        updateStatsPanel();
        load();
    }

    @Subscribe(id = "showFeedback", subject = "clickListener")
    public void onShowFeedbackClick(final ClickEvent<JmixButton> event) {
        Set<ScannerInitializer> selectedItems = scannerInitializerDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()){
            return;
        }
        VerticalLayout fbLayout = new VerticalLayout();
        selectedItems.iterator().next().getFeedback().forEach(fb -> fbLayout.add(new Span(fb)));
        dialogs.createMessageDialog()
                .withCloseOnOutsideClick(true)
                .withCloseOnEsc(true)
                .withResizable(true)
                .withHeader("Feedback")
                .withContent(fbLayout)
                .open();
    }

    @Subscribe(id = "removeStoppedBtn", subject = "clickListener")
    public void onRemoveStoppedBtnClick1(final ClickEvent<JmixButton> event) {
        dialogs.createOptionDialog()
                .withHeader("Please confirm")
                .withText("Do you really want to remove the stopped scanners?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> {
                                    for (ScannerInitializer scannerInitializer : scannerManager.getStoppedScanners()) {
                                            try {
                                                dataManager.remove(scannerInitializer);
                                            } catch (Exception ex) {
                                                log.error(ex.getMessage());
                                            }
                                    }
                                }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }
}