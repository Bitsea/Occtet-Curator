package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.occtet.boc.service.IWorkDataProcessor;

/**
 * base class for work data which is sent to microservices inside a WorkTask.
 * For your own microservice or type of work data, add a subclass and implement the process method with just return processor.process(this);
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AIAnswerWorkData.class, name = "ai_answer"),
        @JsonSubTypes.Type(value = AILicenseMatcherWorkData.class, name = "licenseMatcher_task"),
        @JsonSubTypes.Type(value = AIStatusQueryWorkData.class, name = "ai_status"),
        @JsonSubTypes.Type(value = ScannerSendWorkData.class, name = "scannerdata_send"),
        @JsonSubTypes.Type(value = AICopyrightFilterWorkData.class, name = "copyrightFilter_task"),
        @JsonSubTypes.Type(value = FossReportServiceWorkData.class, name = "fossreport_task"),
        @JsonSubTypes.Type(value = SpdxWorkData.class, name = "spdx_task"),
        @JsonSubTypes.Type(value = SampleWorkData.class, name = "sample"),
        @JsonSubTypes.Type(value = VulnerabilityServiceWorkData.class, name = "vulnerability_task")
})
public abstract class BaseWorkData {
    public abstract boolean process(IWorkDataProcessor processor);

}
