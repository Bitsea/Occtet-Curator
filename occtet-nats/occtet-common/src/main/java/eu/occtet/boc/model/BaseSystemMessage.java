package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StatusDescriptor.class, name = "status"),
        @JsonSubTypes.Type(value = MicroserviceDescriptor.class, name = "descriptor")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseSystemMessage {

}
