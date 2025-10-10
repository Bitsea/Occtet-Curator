package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.License;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseFactory {

    @Autowired
    private DataManager dataManager;

    public License create(@Nonnull Integer priority, @Nonnull String licenseType, @Nonnull String licenseText,
                          @Nonnull String licenseName, @Nonnull String detailsUrl, boolean isModified,
                          boolean curated, boolean isSpdx){
        License license = dataManager.create(License.class);

        license.setPriority(priority);
        license.setLicenseType(licenseType);
        license.setLicenseText(licenseText);
        license.setLicenseName(licenseName);
        license.setDetailsUrl(detailsUrl);
        license.setModified(isModified);
        license.setCurated(curated);
        license.setSpdx(isSpdx);

        return dataManager.save(license);
    }

    public License create(@Nonnull String licenseType, @Nonnull String licenseText, @Nonnull String licenseName){
        return create(0, licenseType, licenseText, licenseName, "", false, false, false);
    }

    public License create(@Nonnull String licenseType,@Nonnull String licenseText,@Nonnull String licenseName, @Nonnull String detailUrl, boolean isSpdx){
        return create(0,licenseType,licenseText,licenseName,detailUrl,false,false,isSpdx);
    }
}