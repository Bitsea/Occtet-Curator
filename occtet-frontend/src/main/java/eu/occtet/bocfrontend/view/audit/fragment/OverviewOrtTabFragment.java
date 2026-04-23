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

package eu.occtet.bocfrontend.view.audit.fragment;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.OrtIssue;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.audit.AuditView;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceLoader;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;



@FragmentDescriptor("OverviewOrtTabFragment.xml")
public class OverviewOrtTabFragment extends Fragment<JmixTabSheet>{

    private static final Logger log = LogManager.getLogger(OverviewOrtTabFragment.class);

    private Project project;
    private View<?> hostView;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionLoader<OrtIssue> ortIssuesDl;

    @ViewComponent
    private  CollectionLoader<OrtViolation> ortViolationsDl;

    @ViewComponent
    private InstanceLoader<OrtIssue> ortIssueDl;

    @ViewComponent
    private InstanceLoader<OrtViolation> ortViolationDl;

    @ViewComponent
    private JmixButton informationIssueButton;

    @ViewComponent
    private JmixButton messageIssueButton;

    @ViewComponent
    private JmixButton informationViolationButton;

    @ViewComponent
    private JmixButton howToFixButton;

    @ViewComponent
    private JmixButton messageViolationButton;

    @ViewComponent
    private TextField howToFixField;

    @ViewComponent
    private TextField messageViolationField;

    @ViewComponent
    private TextField messageIssueField;

    @Autowired
    private Dialogs dialogs;

    private InventoryItem issueItem;
    private InventoryItem violationItem;

    public void setProjectOrtOverview(@Nonnull Project project){
        this.project = dataContext.merge(project);
        setOrtInformation(this.project);
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }

    @Subscribe("issuesDataGrid")
    public void clickOnOrtIssuesDatagrid(final ItemClickEvent<OrtIssue> event){

        OrtIssue issue = event.getItem();
        if(issue != null){

            informationIssueButton.setEnabled(false);
            messageIssueButton.setEnabled(false);

            if(issue.getInventoryItem() != null){
                informationIssueButton.setEnabled(true);
                issueItem = issue.getInventoryItem();
            }
            if(issue.getMessage() != null){
                if(!issue.getMessage().isEmpty()) {
                    messageIssueButton.setEnabled(true);
                }
            }
            ortIssueDl.setEntityId(issue);
            ortIssueDl.load();
        }
    }

    @Subscribe("violationsDataGrid")
    public void clickOnOrtViolationsDatagrid(final ItemClickEvent<OrtViolation> event){

        OrtViolation violation = event.getItem();
        if(violation != null){

            howToFixButton.setEnabled(false);
            messageViolationButton.setEnabled(false);
            informationViolationButton.setEnabled(false);

            if(violation.getInventoryItem() != null){
                informationViolationButton.setEnabled(true);
                violationItem = violation.getInventoryItem();
            }
            if(violation.getHowToFix() != null){
                if(!violation.getHowToFix().isEmpty()){
                    howToFixButton.setEnabled(true);
                }
            }
            if(violation.getMessage() != null){
                if(!violation.getMessage().isEmpty()){
                    messageViolationButton.setEnabled(true);
                }
            }
            ortViolationDl.setEntityId(violation);
            ortViolationDl.load();
        }
    }

    @Subscribe(id = "informationIssueButton", subject = "clickListener")
    public void onInformationIssueItemButtonClick(final ClickEvent<JmixButton> event) {
        if(issueItem != null){
            openInventoryTab(issueItem);
        }
    }

    @Subscribe(id = "informationViolationButton", subject = "clickListener")
    public void onInformationViolationItemButtonClick(final ClickEvent<JmixButton> event) {
        if(violationItem != null){
            openInventoryTab(violationItem);
        }
    }

    @Subscribe(id = "messageIssueButton", subject = "clickListener")
    public void onMessageIssueButtonClick(final ClickEvent<JmixButton> event) {
        openInformationDialog("Message",messageIssueField.getValue());
    }

    @Subscribe(id = "howToFixButton", subject = "clickListener")
    public void onHowToFixButtonClick(final ClickEvent<JmixButton> event) {
        openInformationDialog("How to fix",howToFixField.getValue());
    }

    @Subscribe(id = "messageViolationButton", subject = "clickListener")
    public void onMessageViolationButtonClick(final ClickEvent<JmixButton> event) {
        openInformationDialog("Message",messageViolationField.getValue());
    }

    private void setOrtInformation(Project project){
        loadOrtIssues(project);
        loadOrtViolations(project);
    }

    private void loadOrtIssues(Project project){
        ortIssuesDl.setParameter("projectID",project.getId());
        ortIssuesDl.load();
    }

    private void loadOrtViolations(Project project){
        ortViolationsDl.setParameter("projectID",project.getId());
        ortViolationsDl.load();
    }

    private void openInformationDialog(String type,String message){
        dialogs.createMessageDialog()
                .withHeader(type)
                .withMaxWidth("30%")
                .withMaxHeight("30%")
                .withText(message)
                .open();
    }

    private void openInventoryTab(InventoryItem item){
        if(hostView instanceof AuditView auditView){
            auditView.handleInventoryItemFromOverview(item);
        }
    }

}
