package eu.occtet.bocfrontend.usermanagement;

import io.jmix.oidc.user.JmixOidcUser;
import io.jmix.oidc.usermapper.OidcUserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class CuratorOidcUserMapper implements OidcUserMapper {

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(OidcUser oidcUser) {

        List<String> roles = oidcUser.getClaimAsStringList("roles");

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String role : roles) {

            switch (role) {

                case "admin":
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_CURATOR"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_READER"));
                    break;

                case "curator":
                    authorities.add(new SimpleGrantedAuthority("ROLE_CURATOR"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_READER"));
                    break;

                case "reader":
                    authorities.add(new SimpleGrantedAuthority("ROLE_READER"));
                    break;
            }
        }

        return authorities;
    }

    @Override
    public JmixOidcUser toJmixUser(OidcUser oidcUser) {
        //TODO what to do here?
        return null;
    }
}