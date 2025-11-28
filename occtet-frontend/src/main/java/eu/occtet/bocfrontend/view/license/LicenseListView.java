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


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.service.SPDXLicenseService;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.services.LicenseTextService;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.DataGridColumn;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.beans.PropertyChangeEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Route(value = "licenses", layout = MainView.class)
@ViewController(id = "License.list")
@ViewDescriptor(path = "license-list-view.xml")
@LookupComponent("licensesDataGrid")
@DialogMode(width = "64em")
public class LicenseListView extends StandardListView<License> {

    private static final Logger log = LogManager.getLogger(LicenseListView.class);

    private final static String SIMILARITY_RANK_COLUMN_ID ="Similarity Rank";
    private static final int MAX_FULLTEXT_RESULTS = 50;

    @ViewComponent
    private CollectionLoader<License> licensesDl;

    @Autowired
    private SPDXLicenseService spdxLicenseService;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private LicenseTextService licenseTextService;
    @ViewComponent
    private DataGrid<License> licensesDataGrid;

    @Autowired
    private Messages messages;

    @ViewComponent
    private CollectionContainer<License> licensesDc;

    @Autowired
    private LicenseRepository licenseRepository;

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
            List<Pair<License, Float>> licenseTypes = new ArrayList<>(licenseTextService.findBySimilarity(search, MAX_FULLTEXT_RESULTS));
            DataGridColumn dgc =licensesDataGrid.addColumn(new TextRenderer<>(entity -> {
                AtomicReference<String> rankLabel = new AtomicReference<>("");
                Optional<Float> rankValue= licenseTypes.stream()
                        .filter(lt -> entity.getLicenseType().equals(lt.getKey().getLicenseType()))
                        .map(Pair::getValue).findFirst();
                rankValue.ifPresent(rank -> rankLabel.set("" + new DecimalFormat("#").format(rank * 100)));
                return rankLabel.get();
            }));
            String key=messages.getMessage(getClass(), "similarityRank");
            if (licensesDataGrid.getColumnByKey(key)==null) {
                dgc.setKey(messages.getMessage(getClass(), "similarityRank"));
                dgc.setHeader(messages.getMessage(getClass(), "similarityRank"));
            }
            licensesDc.setItems(licenseTypes.stream().map(Pair::getKey).collect(Collectors.toList()));
        } else {
            log.debug("empty search, loading all licenses");
            licensesDc.setItems( licenseRepository.findAll());
        }
    }

    private void checkAndRemoveSimilarityColumn() {
        String key=messages.getMessage(getClass(), "similarityRank");
        if (licensesDataGrid.getColumnByKey(key)!=null)
            licensesDataGrid.removeColumnByKey(key);
    }

    @Subscribe("fetchSPDXButton")
    public void fetchSPDX_Licenses(ClickEvent<Button> event){
        checkAndRemoveSimilarityColumn();
        spdxLicenseService.readDefaultLicenseInfos();
        licensesDl.load();
    }






}