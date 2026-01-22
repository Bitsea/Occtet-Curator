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

package eu.occtet.bocfrontend.view.appconfiguration;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.AppConfigurationRepository;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigKey;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfiguration;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "configuration-view", layout = MainView.class)
@ViewController(id = "ConfigurationView")
@ViewDescriptor(path = "configuration-view.xml")
public class ConfigurationView extends StandardView {


    @Autowired
    private AppConfigurationRepository repository;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Fragments fragments;
    @Autowired
    private Messages messages;

    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private JmixButton saveBtn;
    @ViewComponent
    private TextField basePathField;
    @ViewComponent
    private JmixTabSheet mainTabSheet;

    private Map<AppConfigKey, AppConfiguration> configMap = new HashMap<>();

    // TODO turn button visible if there were changes done

    @Subscribe
    public void onInit(InitEvent event) {
        loadConfigurations();
    }

    private void loadConfigurations() {
        List<AppConfiguration> configs = repository.findAll();

        for (AppConfiguration config : configs) {
            AppConfiguration tracked = dataContext.merge(config);
            configMap.put(tracked.getConfigKey(), tracked);
        }

        bindField(AppConfigKey.GENERAL_BASE_PATH, basePathField);
    }

    private void bindField(AppConfigKey key, com.vaadin.flow.component.AbstractField<?, String> field) {
        AppConfiguration config = configMap.get(key);
        if (config != null) {
            field.setValue(config.getValue() != null ? config.getValue() : "");

            field.addValueChangeListener(e -> config.setValue(e.getValue()));
        }
    }

    /**
     * Listener to enable save button immediately when user types fields
     */
    @Subscribe(target = Target.DATA_CONTEXT)
    public void onChange(DataContext.ChangeEvent event) {
        updateSaveButtonState();
    }

    /**
     * Listener to hide save button in Editor tab.
     */
    @Subscribe("mainTabSheet")
    public void onMainTabSheetSelectedChange(JmixTabSheet.SelectedChangeEvent event) {
        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        if ("editor".equals(mainTabSheet.getSelectedTab().getId().orElse(""))) {
            saveBtn.setVisible(false);
        } else {
            saveBtn.setVisible(true);
            saveBtn.setEnabled(dataContext.hasChanges());
        }
    }

    @Subscribe("saveBtn")
    public void onSaveBtnClick(ClickEvent<JmixButton> event) {
        if (dataContext.hasChanges()) {
            dataContext.save();
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.appconfiguration/configurationView" +
                            ".saveSuccess"))
                    .withType(Notifications.Type.SUCCESS)
                    .show();
            updateSaveButtonState();
        } else {
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.appconfiguration/configurationView.noChanges"))
                    .withType(Notifications.Type.DEFAULT)
                    .show();
        }
    }
}