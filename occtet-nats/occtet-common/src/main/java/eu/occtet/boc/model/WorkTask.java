package eu.occtet.boc.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * the WorkTask which will be sent to microservices. See BaseWorkData for how to transfer actual data.
 * @param task unique name of the task the microservice should execute
 * @param details (optional) details (only a string)
 * @param timestamp set by the sender
 * @param workData required data for executing the task. Use the appropriate subclass of BaseWorkData or create your own for your task/microservice
 */
@JsonDeserialize
public record WorkTask(String task,String details,long timestamp, BaseWorkData workData) {
}
