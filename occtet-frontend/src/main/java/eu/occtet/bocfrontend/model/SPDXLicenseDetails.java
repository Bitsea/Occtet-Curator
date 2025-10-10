package eu.occtet.bocfrontend.model;

public class SPDXLicenseDetails {
    private boolean isDeprecatedLicenseId;

    private String licenseText;

    private String standardLicenseTemplate;

    private String name;

    private String licenseId;

    private String licenseTextHtml;

    public SPDXLicenseDetails() {
    }

    public SPDXLicenseDetails(String licenseText, String name, String licenseId) {
        this.licenseText = licenseText;
        this.name = name;
        this.licenseId = licenseId;
    }

    public boolean isDeprecatedLicenseId() {
        return isDeprecatedLicenseId;
    }

    public void setDeprecatedLicenseId(boolean deprecatedLicenseId) {
        isDeprecatedLicenseId = deprecatedLicenseId;
    }

    public String getStandardLicenseTemplate() {
        return standardLicenseTemplate;
    }

    public void setStandardLicenseTemplate(String standardLicenseTemplate) {
        this.standardLicenseTemplate = standardLicenseTemplate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseTextHtml() {
        return licenseTextHtml;
    }

    public void setLicenseTextHtml(String licenseTextHtml) {
        this.licenseTextHtml = licenseTextHtml;
    }

    public String getLicenseText() {
        return licenseText;
    }

    public void setLicenseText(String licenseText) {
        this.licenseText = licenseText;
    }
}
