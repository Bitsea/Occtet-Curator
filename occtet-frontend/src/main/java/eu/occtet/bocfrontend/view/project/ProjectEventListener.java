package eu.occtet.bocfrontend.view.project;

import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.ProjectMember;
import eu.occtet.bocfrontend.entity.User;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
public class ProjectEventListener {
    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @EventListener
    public void onProjectCreated(EntityChangedEvent<Project> event) {

        if (event.getType() != EntityChangedEvent.Type.CREATED) {
            return;
        }

        Id<Project> projectId = event.getEntityId();

        Project project = dataManager.load(Project.class)
                .id(projectId)
                .one();

        ProjectMember member = dataManager.create(ProjectMember.class);

        member.setProject(project);
        member.setUsername((User) currentAuthentication.getUser());


        dataManager.save(member);
    }
}
