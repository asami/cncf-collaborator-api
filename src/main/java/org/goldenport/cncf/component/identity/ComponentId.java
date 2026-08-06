package org.goldenport.cncf.component.identity;

import java.util.Objects;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentId {
    private final ComponentNamespace _namespace;
    private final ComponentLocalId _local_id;

    public ComponentId(ComponentNamespace namespace, ComponentLocalId localid) {
        _namespace = Objects.requireNonNull(namespace, "namespace is required");
        _local_id = Objects.requireNonNull(localid, "local ID is required");
    }

    public static ComponentId of(ComponentNamespace namespace, ComponentLocalId localid) {
        return new ComponentId(namespace, localid);
    }

    public static ComponentIdentityResult<ComponentId> parse(String qualifiedid) {
        if (qualifiedid == null) {
            return ComponentIdentityResult.failure(new ComponentIdentityResult.Error(
                    "component.identity.id.required", "component ID is required"));
        }
        int separator = qualifiedid.lastIndexOf('.');
        if (separator <= 0 || separator == qualifiedid.length() - 1) {
            return ComponentIdentityResult.failure(new ComponentIdentityResult.Error(
                    "component.identity.id.qualified",
                    "component ID must be namespace-qualified: " + qualifiedid));
        }
        ComponentIdentityResult<ComponentNamespace> namespace =
                ComponentNamespace.parse(qualifiedid.substring(0, separator));
        if (namespace.isFailure()) {
            return ComponentIdentityResult.failure(namespace.error().orElseThrow());
        }
        ComponentIdentityResult<ComponentLocalId> localid =
                ComponentLocalId.parse(qualifiedid.substring(separator + 1));
        if (localid.isFailure()) {
            return ComponentIdentityResult.failure(localid.error().orElseThrow());
        }
        return ComponentIdentityResult.success(new ComponentId(
                namespace.value().orElseThrow(), localid.value().orElseThrow()));
    }

    public static ComponentId require(String qualifiedid) {
        return parse(qualifiedid).requireValue();
    }

    public ComponentNamespace namespace() {
        return _namespace;
    }

    public ComponentLocalId localId() {
        return _local_id;
    }

    public String qualifiedName() {
        return _namespace.value() + "." + _local_id.value();
    }

    @Override
    public String toString() {
        return qualifiedName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ComponentId that
                && _namespace.equals(that._namespace)
                && _local_id.equals(that._local_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_namespace, _local_id);
    }
}
