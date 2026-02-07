package org.goldenport.cncf.collaborator.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public final class DefaultActionCall implements ActionCall {
    private final String operationName;
    private final Map<String, Object> arguments;

    public DefaultActionCall(String operationName, Map<String, Object> arguments) {
        this.operationName = Objects.requireNonNull(operationName, "operationName is required");
        Map<String, Object> copy = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
        this.arguments = Collections.unmodifiableMap(copy);
    }

    @Override
    public String operationName() {
        return operationName;
    }

    @Override
    public Map<String, Object> arguments() {
        return arguments;
    }
}
