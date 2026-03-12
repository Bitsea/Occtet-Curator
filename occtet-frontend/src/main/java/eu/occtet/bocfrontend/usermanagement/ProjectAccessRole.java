package eu.occtet.bocfrontend.usermanagement;

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(name = "projectAccess", code = "projectAccess")
public interface ProjectAccessRole {

    @JpqlRowLevelPolicy(
            entityClass = Project.class,
            where = "{E}.id in (select pa.project.id from ProjectAccess pa where pa.username = :current_user_username)"
    )
    void projectPolicy();
}
