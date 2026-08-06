package org.goldenport.cncf.component.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentIdentityProjection {
    private final ComponentId _component_id;
    private final String _path_segment;
    private final String _lower_flat_local_id;

    public ComponentIdentityProjection(ComponentId componentid) {
        _component_id = Objects.requireNonNull(componentid, "component ID is required");
        _path_segment = _kebab_case(componentid.localId().value());
        _lower_flat_local_id = componentid.localId().value().toLowerCase(Locale.ROOT);
    }

    public static ComponentIdentityProjection of(ComponentId componentid) {
        return new ComponentIdentityProjection(componentid);
    }

    public ComponentId componentId() {
        return _component_id;
    }

    public String qualifiedId() {
        return _component_id.qualifiedName();
    }

    public String mavenGroupId() {
        return _component_id.namespace().value();
    }

    public String mavenArtifactId() {
        return _component_id.namespace().finalSegment() + "-" + _path_segment;
    }

    public String jvmPackage() {
        return mavenGroupId() + "." + _lower_flat_local_id;
    }

    public String generatedClassName() {
        return _component_id.localId().value() + "Component";
    }

    public String pathSegment() {
        return _path_segment;
    }

    public ComponentIdentityResult<String> carFilename(String release) {
        ComponentIdentityResult.Error error = _release_error(release);
        if (error != null) {
            return ComponentIdentityResult.failure(error);
        }
        return ComponentIdentityResult.success(mavenArtifactId() + "-" + release + ".car");
    }

    public String requireCarFilename(String release) {
        return carFilename(release).requireValue();
    }

    public ComponentIdentityResult<String> mavenCoordinate(String scalasuffix, String release) {
        ComponentIdentityResult.Error scalaerror = _scala_suffix_error(scalasuffix);
        if (scalaerror != null) {
            return ComponentIdentityResult.failure(scalaerror);
        }
        ComponentIdentityResult.Error releaseerror = _release_error(release);
        if (releaseerror != null) {
            return ComponentIdentityResult.failure(releaseerror);
        }
        return ComponentIdentityResult.success(mavenGroupId() + ":" + mavenArtifactId() + "_"
                + scalasuffix + ":" + release);
    }

    public String requireMavenCoordinate(String scalasuffix, String release) {
        return mavenCoordinate(scalasuffix, release).requireValue();
    }

    public static ComponentIdentityResult<List<ComponentId>> validateNoScopedCollisions(
            Collection<ComponentId> componentids) {
        if (componentids == null) {
            return ComponentIdentityResult.failure(new ComponentIdentityResult.Error(
                    "component.identity.components.required", "component IDs are required"));
        }
        List<ComponentId> admitted = new ArrayList<>(componentids);
        for (ComponentId componentid : admitted) {
            if (componentid == null) {
                return ComponentIdentityResult.failure(new ComponentIdentityResult.Error(
                        "component.identity.component.required", "component ID is required"));
            }
        }
        admitted.sort(Comparator.comparing(ComponentId::qualifiedName));
        Map<String, ComponentId> mavenkeys = new LinkedHashMap<>();
        Map<String, ComponentId> jvmkeys = new LinkedHashMap<>();
        Map<String, ComponentId> generatedapikeys = new LinkedHashMap<>();
        Map<String, ComponentId> localpathkeys = new LinkedHashMap<>();
        for (ComponentId componentid : admitted) {
            ComponentIdentityProjection projection = of(componentid);
            ComponentIdentityResult.Error collision = _collision_error(
                    "Maven artifact", projection.mavenGroupId() + ":" + projection.mavenArtifactId(),
                    componentid, mavenkeys);
            if (collision != null) {
                return ComponentIdentityResult.failure(collision);
            }
            collision = _collision_error("JVM package", projection.jvmPackage(), componentid, jvmkeys);
            if (collision != null) {
                return ComponentIdentityResult.failure(collision);
            }
            collision = _collision_error(
                    "generated API", projection.jvmPackage() + ":" + projection.generatedClassName(),
                    componentid, generatedapikeys);
            if (collision != null) {
                return ComponentIdentityResult.failure(collision);
            }
            collision = _collision_error(
                    "normalized local path", projection.mavenGroupId() + ":" + projection.pathSegment(),
                    componentid, localpathkeys);
            if (collision != null) {
                return ComponentIdentityResult.failure(collision);
            }
        }
        return ComponentIdentityResult.success(List.copyOf(admitted));
    }

    public static List<ComponentId> requireNoScopedCollisions(Collection<ComponentId> componentids) {
        return validateNoScopedCollisions(componentids).requireValue();
    }

    private static ComponentIdentityResult.Error _collision_error(
            String scope, String key, ComponentId componentid, Map<String, ComponentId> identities) {
        ComponentId previous = identities.putIfAbsent(key, componentid);
        if (previous != null && !previous.equals(componentid)) {
            return new ComponentIdentityResult.Error(
                    "component.identity.projection.collision",
                    "component identity collision for " + scope + " projection key " + key + ": "
                            + previous.qualifiedName() + " and " + componentid.qualifiedName());
        }
        return null;
    }

    private static String _kebab_case(String localid) {
        StringBuilder builder = new StringBuilder(localid.length() + 8);
        for (int index = 0; index < localid.length(); index++) {
            char current = localid.charAt(index);
            if (index > 0 && _is_word_boundary(localid, index)) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }

    private static boolean _is_word_boundary(String value, int index) {
        char previous = value.charAt(index - 1);
        char current = value.charAt(index);
        if (!Character.isUpperCase(current)) {
            return false;
        }
        if (Character.isLowerCase(previous) || Character.isDigit(previous)) {
            return true;
        }
        return index > 1
                && Character.isUpperCase(previous)
                && Character.isUpperCase(value.charAt(index - 2))
                && index + 1 < value.length()
                && Character.isLowerCase(value.charAt(index + 1));
    }

    private static ComponentIdentityResult.Error _non_blank_error(
            String value, String code, String name) {
        if (value == null || value.isBlank()) {
            return new ComponentIdentityResult.Error(code, name + " is required and must not be blank");
        }
        return null;
    }

    private static ComponentIdentityResult.Error _release_error(String release) {
        ComponentIdentityResult.Error error = _non_blank_error(
                release, "component.identity.release.required", "release");
        if (error != null) {
            return error;
        }
        if (!release.matches("[A-Za-z0-9][A-Za-z0-9._+-]*")) {
            return new ComponentIdentityResult.Error(
                    "component.identity.release.format",
                    "release must be a portable opaque release token: " + release);
        }
        return null;
    }

    private static ComponentIdentityResult.Error _scala_suffix_error(String scalasuffix) {
        ComponentIdentityResult.Error error = _non_blank_error(
                scalasuffix, "component.identity.scala-suffix.required", "Scala suffix");
        if (error != null) {
            return error;
        }
        if (!scalasuffix.matches("[0-9]+(?:\\.[0-9]+)*")) {
            return new ComponentIdentityResult.Error(
                    "component.identity.scala-suffix.format",
                    "Scala suffix must be a bare binary-version token: " + scalasuffix);
        }
        return null;
    }
}
