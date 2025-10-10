
package eu.occtet.bocfrontend.entity;

import org.springframework.lang.Nullable;


public enum ScannerInitializerStatus {
    CREATING("CREATING"),APPROVE("APPROVE"),IN_PROGRESS("IN_PROGRESS"),WAITING("WAITING"), STOPPED("STOPPED"), COMPLETED("COMPLETED");

    private final String id;

    ScannerInitializerStatus(String value) {
        this.id = value;
    }


    public String getId() {
        return id;
    }

    @Nullable
    public static ScannerInitializerStatus fromId(String id) {
        for (ScannerInitializerStatus at : ScannerInitializerStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}


