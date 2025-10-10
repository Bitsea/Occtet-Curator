package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SPDXLicenseDetails(
        boolean isOsiApproved,
        boolean isDeprecatedLicenseId,
        String licenseText,
        String standardLicenseTemplate,
        String name,
        String licenseId,
        String licenseTextHtml) { }
