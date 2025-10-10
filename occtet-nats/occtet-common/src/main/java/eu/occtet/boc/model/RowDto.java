package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RowDto(
        @JsonProperty("Issue #")
        @JsonAlias({"Issue#", "Issue"})
        String rowTitle,

        @JsonProperty("Name")
        @JsonAlias({"Name (in tool)"})
        String componentNameAndVersion,

        @JsonProperty("Parent name")
        @JsonAlias({"Parent name\n", "Parent name\n\n", "Parent Name"})
        String parentNameAndVersion,

        @JsonProperty("Component Indicator")
        @JsonAlias({"Component Indicator\n"})
        String componentIndicator,

        @JsonProperty("Priority")
        Integer priority,

        @JsonProperty("License Type")
        @JsonAlias({"LicenseType"})
        String licenseTypeId,

        @JsonProperty("License Text")
        @JsonAlias({"LicenseText"})
        String licenseText,

        @JsonProperty("URL")
        String URL,

        @JsonProperty("# Files")
        @JsonAlias({"#Files", "Files count"})
        Integer size,

        @JsonProperty("Files")
        String files,

        @JsonProperty("External Notes")
        @JsonAlias({"ExternalNotes", "Notes"})
        String externalNotes,

        @JsonProperty("Vulnerability List (CVSS Severity Score, Severity Impact, CVE Dictionary Entry, URL)")
        @JsonAlias({"Vulnerability List", "Vulnerabilities"})
        String vulnerabilityList,

        @JsonProperty("Copyright")
        String copyright,

        @JsonProperty("Linking")
        String linking,

        @JsonProperty("Encryption")
        String encryption
) {}
