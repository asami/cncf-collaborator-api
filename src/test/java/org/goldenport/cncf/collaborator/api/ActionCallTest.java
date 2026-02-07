package org.goldenport.cncf.collaborator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionCallTest {
    @Test
    void defaultActionCallExposesOperationNameAndArguments() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("key", "value");
        ActionCall call = new DefaultActionCall("describe", arguments);

        assertEquals("describe", call.operationName());
        assertEquals("value", call.arguments().get("key"));
        assertThrows(UnsupportedOperationException.class, () -> call.arguments().put("key", "other"));
    }
}
