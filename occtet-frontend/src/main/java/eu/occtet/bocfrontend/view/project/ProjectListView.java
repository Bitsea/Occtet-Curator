package eu.occtet.bocfrontend.view.project;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

import java.util.ArrayList;
import java.util.List;


@Route(value = "projects", layout = MainView.class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
public class ProjectListView extends StandardListView<Project> {


    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private CollectionContainer<Project> projectsDc;

    @ViewComponent
    private CollectionLoader<Project> projectsDl;


    @Subscribe("searchButton")
    public void searchProjects(ClickEvent<Button> event){

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){

            List<Project> projects = projectsDc.getItems();
            List<Project> searchProjects = new ArrayList<>();

            for(Project project : projects){
                if(project.getProjectName().toUpperCase().contains(searchWord.toUpperCase()))
                    searchProjects.add(project);
            }
            projectsDc.setItems(searchProjects);
        }else if(searchWord.isEmpty() && event != null){
            projectsDl.load();
        }
    }
}