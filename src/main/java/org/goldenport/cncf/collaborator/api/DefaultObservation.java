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
public final class DefaultObservation implements Observation {
    private final String code;
    private final String message;
    private final Map<String, Object> attributes;

    public DefaultObservation(String code, String message, Map<String, Object> attributes) {
        this.code = Objects.requireNonNull(code, "code is required");
        this.message = Objects.requireNonNull(message, "message is required");
        Map<String, Object> copy = attributes == null ? Map.of() : new LinkedHashMap<>(attributes);
        this.attributes = Collections.unmodifiableMap(copy);
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Map<String, Object> attributes() {
        return attributes;
    }
}
