package org.goldenport.cncf.component.identity;

import java.util.Objects;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentInstanceId {
    public static final String DEFAULT_LABEL = "default";

    private final ComponentId _component_id;
    private final String _label;

    private ComponentInstanceId(ComponentId componentid, String label) {
        _component_id = componentid;
        _label = label;
    }

    public static ComponentIdentityResult<ComponentInstanceId> of(
            ComponentId componentid, String label) {
        ComponentIdentityResult.Error error = _validation_error(componentid, label);
        if (error != null) {
            return ComponentIdentityResult.failure(error);
        }
        return ComponentIdentityResult.success(new ComponentInstanceId(componentid, label));
    }

    public static ComponentInstanceId require(ComponentId componentid, String label) {
        return of(componentid, label).requireValue();
    }

    public static ComponentIdentityResult<ComponentInstanceId> defaultInstance(
            ComponentId componentid) {
        return of(componentid, DEFAULT_LABEL);
    }

    public static ComponentInstanceId requireDefaultInstance(ComponentId componentid) {
        return defaultInstance(componentid).requireValue();
    }

    public ComponentId componentId() {
        return _component_id;
    }

    public String label() {
        return _label;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ComponentInstanceId that
                && _component_id.equals(that._component_id)
                && _label.equals(that._label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_component_id, _label);
    }

    @Override
    public String toString() {
        return _component_id.qualifiedName() + "@" + _label;
    }

    private static ComponentIdentityResult.Error _validation_error(
            ComponentId componentid, String label) {
        if (componentid == null) {
            return new ComponentIdentityResult.Error(
                    "component.identity.instance.component-id.required", "component ID is required");
        }
        if (label == null || label.isEmpty()) {
            return new ComponentIdentityResult.Error(
                    "component.identity.instance.label.required", "instance label is required");
        }
        return null;
    }
}
