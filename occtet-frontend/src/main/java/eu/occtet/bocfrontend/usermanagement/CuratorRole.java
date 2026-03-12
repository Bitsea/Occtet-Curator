package eu.occtet.bocfrontend.usermanagement;

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;

@ResourceRole(name="Curator", code = "curator")
public interface CuratorRole {

    @EntityPolicy(entityClass = Project.class,
            actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    void projectUpdate();
}
