package org.goldenport.cncf.component.identity;

import java.util.Set;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentNamespace {
    private static final Set<String> RESERVED_SEGMENTS = Set.of(
            "abstract", "as", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "def", "derives", "do",
            "double", "else", "end", "enum", "erased", "export", "exports", "extends",
            "extension", "false", "final", "finally", "float", "for", "given",
            "goto", "if", "implements", "implicit", "import", "infix", "inline", "instanceof",
            "int", "interface", "lazy", "long", "macro", "match", "module", "native", "new",
            "null", "object", "opaque", "open", "opens", "override", "package", "permits",
            "private", "protected", "provides", "public", "record", "requires", "return", "sealed",
            "short", "static", "strictfp", "super", "switch", "synchronized", "then", "this",
            "throw", "throws", "to", "trait", "transient", "transitive", "transparent", "true",
            "try", "type", "uses", "using", "val", "var", "void", "volatile", "when", "while",
            "with", "yield");

    private final String _value;

    private ComponentNamespace(String value) {
        _value = value;
    }

    public static ComponentIdentityResult<ComponentNamespace> parse(String value) {
        ComponentIdentityResult.Error error = _validation_error(value);
        if (error != null) {
            return ComponentIdentityResult.failure(error);
        }
        return ComponentIdentityResult.success(new ComponentNamespace(value));
    }

    public static ComponentNamespace require(String value) {
        return parse(value).requireValue();
    }

    public String value() {
        return _value;
    }

    public String finalSegment() {
        return _value.substring(_value.lastIndexOf('.') + 1);
    }

    @Override
    public String toString() {
        return _value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ComponentNamespace that && _value.equals(that._value);
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }

    private static ComponentIdentityResult.Error _validation_error(String value) {
        if (value == null) {
            return new ComponentIdentityResult.Error(
                    "component.identity.namespace.required", "namespace is required");
        }
        String[] segments = value.split("\\.", -1);
        if (segments.length < 2) {
            return new ComponentIdentityResult.Error(
                    "component.identity.namespace.segment-count",
                    "namespace must have at least two segments: " + value);
        }
        for (String segment : segments) {
            if (!segment.matches("[a-z][a-z0-9]*")) {
                return new ComponentIdentityResult.Error(
                        "component.identity.namespace.segment-format",
                        "invalid namespace segment: " + segment);
            }
            if (RESERVED_SEGMENTS.contains(segment)) {
                return new ComponentIdentityResult.Error(
                        "component.identity.namespace.segment-reserved",
                        "reserved namespace segment: " + segment);
            }
        }
        return null;
    }
}
