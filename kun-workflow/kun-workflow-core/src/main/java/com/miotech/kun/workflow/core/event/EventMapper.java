package com.miotech.kun.workflow.core.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.miotech.kun.commons.pubsub.event.Event;

public class EventMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerSubtypes(new NamedType(LineageEvent.class, "LineageEvent"));
        MAPPER.registerSubtypes(new NamedType(TickEvent.class, "TickEvent"));
        MAPPER.registerSubtypes(new NamedType(TaskAttemptStatusChangeEvent.class, "TaskAttemptStatusChangeEvent"));
        MAPPER.registerSubtypes(new NamedType(TaskAttemptFinishedEvent.class, "TaskAttemptFinishedEvent"));
        MAPPER.registerSubtypes(new NamedType(TaskRunCreatedEvent.class, "TaskAttemptFinishedEvent"));
    }

    public static String toJson(Event event) throws JsonProcessingException {
        return MAPPER.writeValueAsString(event);
    }

    public static Event toEvent(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, Event.class);
    }
}
