package org.goldenport.cncf.component.identity;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentLocalId {
    private final String _value;

    private ComponentLocalId(String value) {
        _value = value;
    }

    public static ComponentIdentityResult<ComponentLocalId> parse(String value) {
        ComponentIdentityResult.Error error = _validation_error(value);
        if (error != null) {
            return ComponentIdentityResult.failure(error);
        }
        return ComponentIdentityResult.success(new ComponentLocalId(value));
    }

    public static ComponentLocalId require(String value) {
        return parse(value).requireValue();
    }

    public String value() {
        return _value;
    }

    @Override
    public String toString() {
        return _value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ComponentLocalId that && _value.equals(that._value);
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }

    private static ComponentIdentityResult.Error _validation_error(String value) {
        if (value == null) {
            return new ComponentIdentityResult.Error(
                    "component.identity.local-id.required", "component local ID is required");
        }
        if (!value.matches("[A-Z][A-Za-z0-9]*")) {
            return new ComponentIdentityResult.Error(
                    "component.identity.local-id.format", "invalid component local ID: " + value);
        }
        return null;
    }
}
