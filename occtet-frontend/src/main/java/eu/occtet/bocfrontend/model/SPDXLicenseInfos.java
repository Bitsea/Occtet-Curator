package eu.occtet.bocfrontend.model;

import eu.occtet.bocfrontend.entity.License;

import java.util.List;

public class SPDXLicenseInfos {

        private String licenseListVersion;
        private List<License> licenses;
        private String releaseDate;

        public String getLicenseListVersion() {
            return licenseListVersion;
        }

        public void setLicenseListVersion(String licenseListVersion) {
            this.licenseListVersion = licenseListVersion;
        }

        public List<License> getLicenses() {
            return licenses;
        }

        public void setLicenses(List<License> licenses) {
            this.licenses = licenses;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }

}
