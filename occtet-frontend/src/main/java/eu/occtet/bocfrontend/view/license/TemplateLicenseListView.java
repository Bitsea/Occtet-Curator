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

package eu.occtet.bocfrontend.view.license;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.TemplateLicenseRepository;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.TemplateLicense;
import eu.occtet.bocfrontend.service.SPDXLicenseService;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.services.LicenseTextService;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.DataGridColumn;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import io.micrometer.common.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "template-licenses", layout = MainView.class)
@ViewController(id = "TemplateLicense.list")
@ViewDescriptor(path = "template-license-list-view.xml")
@LookupComponent("licensesDataGrid")
@DialogMode(width = "80%", height = "80%")
public class TemplateLicenseListView extends StandardListView<TemplateLicense> {

    private static final Logger log = LogManager.getLogger(TemplateLicenseListView.class);

    private final static String SIMILARITY_RANK_COLUMN_ID = "Similarity Rank";
    private static final int MAX_FULLTEXT_RESULTS = 50;

    @ViewComponent
    private CollectionLoader<TemplateLicense> licensesDl;

    @Autowired
    private SPDXLicenseService spdxLicenseService;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private LicenseTextService licenseTextService;

    @ViewComponent
    private DataGrid<TemplateLicense> licensesDataGrid;

    @Autowired
    private Messages messages;

    @ViewComponent
    private CollectionContainer<TemplateLicense> licensesDc;

    @Autowired
    private TemplateLicenseRepository templateLicenseRepository;

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @Autowired
    private ProjectRepository projectRepository;

    @ViewComponent
    private HorizontalLayout filterBox;

    @Subscribe
    public void onInit(InitEvent event) {
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(project -> project.getProjectName() + " - " + project.getVersion());
    }

    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (event != null) {
            List<TemplateLicense> licensesProject = templateLicenseRepository.findTemplateLicensesByProject(event.getValue());
            loadLicenses(licensesProject);
            filterBox.setVisible(!licensesProject.isEmpty());
        }
    }

    @Subscribe(id = "textSearchLicense", subject = "singleClickListener")
    public void onTextSearchLicenseClick(final ClickEvent<JmixButton> event) {
        checkAndRemoveSimilarityColumn();
        log.debug("clicked textsearchbutton");
        DialogWindow<LicenseDialogWindowView> window =
                dialogWindows.view(this, LicenseDialogWindowView.class).build();
        window.setResizable(true);
        window.setWidth("70%");
        window.setHeight("70%");
        window.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.SAVE)) {
                doFulltextSearch(window.getView().getText());
            }
        });
        window.open();
    }

    private void doFulltextSearch(String search) {
        if (!StringUtils.isEmpty(search)) {

            List<Pair<TemplateLicense, Float>> licenseTypes = licenseTextService.findBySimilarity(search, MAX_FULLTEXT_RESULTS);

            Map<String, Float> similarityMap = licenseTypes.stream()
                    .collect(Collectors.toMap(
                            pair -> pair.getKey().getLicenseType(),
                            Pair::getValue,
                            (existing, replacement) -> existing // Keeps the first one if there are duplicate license types
                    ));

            DataGridColumn<TemplateLicense> dgc = licensesDataGrid.addColumn(new TextRenderer<>(entity -> {
                Float rank = similarityMap.get(entity.getLicenseType());
                return rank != null ? new DecimalFormat("#").format(rank * 100) : "";
            }));
            String key = messages.getMessage(getClass(), "similarityRank");
            if (licensesDataGrid.getColumnByKey(key) == null) {
                dgc.setKey(messages.getMessage(getClass(), "similarityRank"));
                dgc.setHeader(messages.getMessage(getClass(), "similarityRank"));
            }
            licensesDc.setItems(licenseTypes.stream().map(Pair::getKey).collect(Collectors.toList()));
        } else {
            log.debug("empty search, loading all licenses");
            licensesDc.setItems(templateLicenseRepository.findAll());
        }
    }

    private void checkAndRemoveSimilarityColumn() {
        String key = messages.getMessage(getClass(), "similarityRank");
        if (licensesDataGrid.getColumnByKey(key) != null)
            licensesDataGrid.removeColumnByKey(key);
    }

    @Subscribe("fetchSPDXButton")
    public void fetchSPDX_Licenses(ClickEvent<Button> event) {
        checkAndRemoveSimilarityColumn();
        spdxLicenseService.readDefaultLicenseInfos();
        licensesDl.load();
    }

    @Subscribe("licensesDataGrid")
    public void clickOnLicenseDatagrid(ItemDoubleClickEvent<TemplateLicense> event) {
        DialogWindow<TemplateLicenseDetailView> window =
                dialogWindows.detail(this, TemplateLicense.class)
                        .withViewClass(TemplateLicenseDetailView.class)
                        .editEntity(event.getItem())
                        .build();
        window.setWidth("100%");
        window.setHeight("100%");
        window.open();
    }

    @Subscribe("showAllButton")
    public void clickOnShowAllButton(ClickEvent<Button> event) {
        List<TemplateLicense> licenses = templateLicenseRepository.findAll();
        loadLicenses(licenses);
        filterBox.setVisible(!licenses.isEmpty());
    }

    private void loadLicenses(List<TemplateLicense> licenses) {
        licensesDl.setParameter("licenses", licenses);
        licensesDl.load();
    }
}