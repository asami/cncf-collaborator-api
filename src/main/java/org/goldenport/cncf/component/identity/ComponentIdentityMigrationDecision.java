package org.goldenport.cncf.component.identity;

import java.util.Objects;
import java.util.Optional;

/*
 * Stable classification returned to runtime and lint consumers.
 *
 * @since   Aug.  8, 2026
 * @version Aug.  8, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentIdentityMigrationDecision {
    public enum Status {
        CANONICAL,
        STRICT_LEGACY,
        MIGRATION_REQUIRED,
        DEFERRED_TO_NEXT_VERSION,
        INVENTORY_ERROR,
        PROJECTION_DISAGREEMENT
    }

    private final Status _status;
    private final String _reason;
    private final String _release;
    private final ComponentIdentityMigrationClassifier.RegistryEntry _entry;
    private final ComponentIdentityProjection _projection;

    ComponentIdentityMigrationDecision(Status status, String reason, String release,
            ComponentIdentityMigrationClassifier.RegistryEntry entry,
            ComponentIdentityProjection projection) {
        _status = Objects.requireNonNull(status, "status is required");
        _reason = Objects.requireNonNull(reason, "reason is required");
        _release = release;
        _entry = entry;
        _projection = projection;
    }

    public Status status() {
        return _status;
    }

    public String reason() {
        return _reason;
    }

    public Optional<String> release() {
        return Optional.ofNullable(_release);
    }

    public Optional<ComponentIdentityMigrationClassifier.RegistryEntry> entry() {
        return Optional.ofNullable(_entry);
    }

    public Optional<ComponentIdentityProjection> projection() {
        return Optional.ofNullable(_projection);
    }
}
