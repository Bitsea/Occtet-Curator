
package eu.occtet.bocfrontend.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum Type implements EnumClass<String> {

    STRING("STRING"),NUMERIC("NUMERIC"),FILE_UPLOAD("FILE_UPLOAD"),BASE_PATH("BASE_PATH"),
    COMMA_SEPARATED_STRING("COMMA_SEPARATED_STRINGS"),BOOLEAN("BOOLEAN");

    private final String id;

    Type(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static Type fromId(String id) {
        for (Type at : Type.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}