package org.goldenport.cncf.component.identity;

import java.util.Objects;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentReleaseCoordinate {
    private final ComponentId _component_id;
    private final String _release;
    private final ComponentIdentityProjection _projection;

    private ComponentReleaseCoordinate(ComponentId componentid, String release,
            ComponentIdentityProjection projection) {
        _component_id = componentid;
        _release = release;
        _projection = projection;
    }

    public static ComponentIdentityResult<ComponentReleaseCoordinate> create(
            ComponentId componentId, String release) {
        if (componentId == null) {
            return ComponentIdentityResult.failure(new ComponentIdentityResult.Error(
                    "component.identity.component.required", "component ID is required"));
        }
        ComponentIdentityProjection projection = ComponentIdentityProjection.of(componentId);
        ComponentIdentityResult<String> filename = projection.carFilename(release);
        if (filename.isFailure()) {
            return ComponentIdentityResult.failure(filename.error().orElseThrow());
        }
        return ComponentIdentityResult.success(
                new ComponentReleaseCoordinate(componentId, release, projection));
    }

    public static ComponentReleaseCoordinate require(ComponentId componentId, String release) {
        return create(componentId, release).requireValue();
    }

    public ComponentId componentId() {
        return _component_id;
    }

    public String release() {
        return _release;
    }

    public String qualifiedId() {
        return _projection.qualifiedId();
    }

    public String mavenGroupId() {
        return _projection.mavenGroupId();
    }

    public String mavenArtifactId() {
        return _projection.mavenArtifactId();
    }

    public String dependencyKey() {
        return qualifiedId() + ":" + _release;
    }

    public String mavenReleaseKey() {
        return mavenGroupId() + ":" + mavenArtifactId() + ":" + _release;
    }

    public String groupPath() {
        return mavenGroupId().replace('.', '/');
    }

    public String carFilename() {
        return _projection.requireCarFilename(_release);
    }

    public String carRepositoryRelativePath() {
        return groupPath() + "/" + mavenArtifactId() + "/" + _release + "/" + carFilename();
    }

    public String carCacheRelativePath() {
        return carRepositoryRelativePath();
    }

    public String carCatalogRelativePath() {
        return "car/" + groupPath() + "/" + mavenArtifactId() + ".yaml";
    }

    public String carIndexKey() {
        return "car:" + mavenGroupId() + ":" + _component_id.localId().value();
    }

    public ComponentIdentityResult<String> integrityKey(String sha256) {
        ComponentIdentityResult.Error error = _sha256_error(sha256);
        if (error != null) {
            return ComponentIdentityResult.failure(error);
        }
        return ComponentIdentityResult.success(mavenReleaseKey() + "@sha256:" + sha256);
    }

    public String requireIntegrityKey(String sha256) {
        return integrityKey(sha256).requireValue();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ComponentReleaseCoordinate that
                && _component_id.equals(that._component_id)
                && _release.equals(that._release);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_component_id, _release);
    }

    @Override
    public String toString() {
        return mavenReleaseKey();
    }

    private static ComponentIdentityResult.Error _sha256_error(String sha256) {
        if (sha256 == null || sha256.isBlank()) {
            return new ComponentIdentityResult.Error(
                    "component.identity.sha256.required", "SHA-256 is required and must not be blank");
        }
        if (!sha256.matches("[0-9a-f]{64}")) {
            return new ComponentIdentityResult.Error(
                    "component.identity.sha256.format",
                    "SHA-256 must be a bare lowercase 64-hex digest: " + sha256);
        }
        return null;
    }
}
