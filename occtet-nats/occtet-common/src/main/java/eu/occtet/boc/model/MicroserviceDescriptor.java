package eu.occtet.boc.model;

public class MicroserviceDescriptor  extends BaseSystemMessage {
    private String name, description,version,acceptableWorkData;
    private UsageType usageType;

    public MicroserviceDescriptor() {
    }

    public MicroserviceDescriptor(String name, String description, String version, String acceptableWorkData, UsageType usageType) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.acceptableWorkData = acceptableWorkData;
        this.usageType = usageType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAcceptableWorkData() {
        return acceptableWorkData;
    }

    public void setAcceptableWorkData(String acceptableWorkData) {
        this.acceptableWorkData = acceptableWorkData;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }
}
