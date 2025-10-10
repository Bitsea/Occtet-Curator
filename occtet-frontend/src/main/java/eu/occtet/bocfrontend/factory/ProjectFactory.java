package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class ProjectFactory {

    @Autowired
    protected DataManager dataManager;

    /**
     * creates project with all attributes
     * @param name
     * @return
     */
    public Project create(@Nonnull String name) {
        Project project = dataManager.create(Project.class);
        project.setProjectName(name);
        return dataManager.save(project);
    }
}
