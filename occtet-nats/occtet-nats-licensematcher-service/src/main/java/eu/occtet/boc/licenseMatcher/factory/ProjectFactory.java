package eu.occtet.boc.licenseMatcher.factory;

import eu.occtet.boc.entity.Project;
import eu.occtet.boc.licenseMatcher.dao.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectFactory {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectFactory(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project create(String projectName){
        Project project;

        if (projectRepository.findByProjectName(projectName).isEmpty()){
            project = new Project(projectName);
        } else {
            project = projectRepository.findByProjectName(projectName).get(0);
        }
        return projectRepository.save(project);
    }
}
