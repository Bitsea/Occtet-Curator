package eu.occtet.bocfrontend.usermanagement;

import eu.occtet.bocfrontend.entity.User;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.UserRepository;
import io.jmix.oidc.claimsmapper.ClaimsRolesMapper;
import io.jmix.security.role.RoleGrantedAuthorityUtils;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class SynchronizingOidcUserMapper extends SynchronizingOidcUserMapper<User> {

    public SynchronizingOidcUserMapper(UnconstrainedDataManager dataManager,
                                         UserRepository userRepository,
                                         ClaimsRolesMapper claimsRolesMapper,
                                         RoleGrantedAuthorityUtils roleGrantedAuthorityUtils) {
        super(dataManager, userRepository, claimsRolesMapper, roleGrantedAuthorityUtils);

        //store role assignments in the database (false by default)
        setSynchronizeRoleAssignments(true);
    }

    @Override
    protected Class<User> getApplicationUserClass() {
        return User.class;
    }

    @Override
    protected void populateUserAttributes(OidcUser oidcUser, User jmixUser) {
        jmixUser.setUsername(oidcUser.getName());
        jmixUser.setFirstName(oidcUser.getGivenName());
        jmixUser.setLastName(oidcUser.getFamilyName());
        jmixUser.setEmail(oidcUser.getEmail());
    }
}