package eu.occtet.bocfrontend.model;

import java.time.LocalDateTime;

public record VexMetadata(LocalDateTime timestamp,
        VexComponent component) { }


class VexComponent {
    private enum type{application, firmware, device, container, platform}
    private String name;
    private String version;

    public VexComponent(){}


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}