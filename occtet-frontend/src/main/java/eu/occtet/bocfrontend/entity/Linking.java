package eu.occtet.bocfrontend.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum Linking implements EnumClass<String> {

    STATIC("Static"),
    DYNAMIC("Dynamic"),
    NONE("None");

    private final String id;

    Linking(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public static Linking fromId(String id) {
        for (Linking linking : Linking.values()) {
            if (linking.getId().equals(id))
                return linking;
        }
        return null;
    }
}
