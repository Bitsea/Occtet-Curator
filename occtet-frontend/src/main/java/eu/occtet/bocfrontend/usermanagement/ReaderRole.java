package eu.occtet.bocfrontend.usermanagement;

import eu.occtet.bocfrontend.entity.Project;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;

@ResourceRole(name="Reader", code="reader")
public interface ReaderRole {

    @EntityPolicy(entityClass = Project.class, actions = EntityPolicyAction.READ)
    void projectRead();
}
