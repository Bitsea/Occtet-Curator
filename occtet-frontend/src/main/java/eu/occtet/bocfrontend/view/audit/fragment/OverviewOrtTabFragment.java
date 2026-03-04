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


import com.vaadin.flow.component.grid.ItemClickEvent;
import eu.occtet.bocfrontend.entity.*;
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
    private JmixButton informationButton;

    @ViewComponent
    private JmixButton messageIssueButton;

    @ViewComponent
    private JmixButton informationViolationButton;

    @ViewComponent
    private JmixButton howToFixButton;

    @ViewComponent
    private JmixButton messageViolationButton;

    @Autowired
    private Dialogs dialogs;

    public void setProjectOrtOverview(@Nonnull Project project){
        this.project = dataContext.merge(project);
        setOrtInformation(this.project);
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
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

    @Subscribe("issuesDataGrid")
    public void clickOnOrtIssuesDatagrid(final ItemClickEvent<OrtIssue> event){

        OrtIssue issue = event.getItem();
        if(issue != null){
            if(issue.getInventoryItem() != null){
                informationButton.setEnabled(true);
                informationButton.addClickListener(e -> {
                    if(hostView instanceof AuditView auditView){
                        auditView.handleInventoryItemFromOverview(issue.getInventoryItem());
                    }
                });
            }
            if(!issue.getMessage().isEmpty()){
                messageIssueButton.setEnabled(true);
                messageIssueButton.addClickListener(e -> openInformationDialog("Message"
                        ,issue.getMessage()));
            }
            ortIssueDl.setEntityId(issue);
            ortIssueDl.load();
        }
    }

    @Subscribe("violationsDataGrid")
    public void clickOnOrtViolationsDatagrid(final ItemClickEvent<OrtViolation> event){

        OrtViolation violation = event.getItem();
        if(violation != null){
            if(violation.getInventoryItem() != null){
                informationViolationButton.setEnabled(true);
                informationButton.addClickListener(e -> {
                    if(hostView instanceof AuditView auditView){
                        auditView.handleInventoryItemFromOverview(violation.getInventoryItem());
                    }
                });
            }
            if(!violation.getHowToFix().isEmpty()){
                howToFixButton.setEnabled(true);
                howToFixButton.addClickListener(e -> openInformationDialog("How to fix"
                        ,violation.getHowToFix()));
            }
            if(!violation.getMessage().isEmpty()){
                messageViolationButton.setEnabled(true);
                messageViolationButton.addClickListener(e -> openInformationDialog("Message"
                        , violation.getMessage()));
            }
            ortViolationDl.setEntityId(violation);
            ortViolationDl.load();
        }
    }

    private void openInformationDialog(String type,String message){
        dialogs.createMessageDialog()
                .withHeader(type)
                .withText(message)
                .open();
    }
}
