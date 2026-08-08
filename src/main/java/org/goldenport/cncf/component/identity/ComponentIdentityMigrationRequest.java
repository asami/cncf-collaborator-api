package org.goldenport.cncf.component.identity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Input evidence for version-sensitive Component identity migration lint.
 *
 * @since   Aug.  8, 2026
 * @version Aug.  8, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentIdentityMigrationRequest {
    private final String _namespace;
    private final String _local_id;
    private final String _legacy_artifact;
    private final String _legacy_local_id;
    private final String _release;
    private final List<AuthoredProjection> _authored_projection_evidence;

    public ComponentIdentityMigrationRequest(String namespace, String localId,
            String legacyArtifact, String legacyLocalId, String release,
            Map<String, String> authoredProjections) {
        this(namespace, localId, legacyArtifact, legacyLocalId, release,
                authoredProjections == null
                        ? List.of()
                        : new LinkedHashMap<>(authoredProjections).entrySet().stream()
                                .map(x -> new AuthoredProjection(x.getKey(), x.getKey(), x.getValue()))
                                .collect(Collectors.toList()));
    }

    public ComponentIdentityMigrationRequest(String namespace, String localId,
            String legacyArtifact, String legacyLocalId, String release,
            List<AuthoredProjection> authoredProjectionEvidence) {
        _namespace = _trim(namespace);
        _local_id = _trim(localId);
        _legacy_artifact = _trim(legacyArtifact);
        _legacy_local_id = _trim(legacyLocalId);
        _release = _trim(release);
        _authored_projection_evidence = authoredProjectionEvidence == null
                ? List.of()
                : List.copyOf(authoredProjectionEvidence);
    }

    public Optional<String> namespace() {
        return Optional.ofNullable(_namespace);
    }

    public Optional<String> localId() {
        return Optional.ofNullable(_local_id);
    }

    public Optional<String> legacyArtifact() {
        return Optional.ofNullable(_legacy_artifact);
    }

    public Optional<String> legacyLocalId() {
        return Optional.ofNullable(_legacy_local_id);
    }

    public Optional<String> release() {
        return Optional.ofNullable(_release);
    }

    public Map<String, String> authoredProjections() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (AuthoredProjection evidence : _authored_projection_evidence) {
            result.put(evidence.key(), evidence.value());
        }
        return Map.copyOf(result);
    }

    public List<AuthoredProjection> authoredProjectionEvidence() {
        return _authored_projection_evidence;
    }

    private static String _trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class AuthoredProjection {
        private final String _key;
        private final String _source;
        private final String _value;

        public AuthoredProjection(String key, String source, String value) {
            _key = Objects.requireNonNull(_trim(key), "projection key is required");
            _source = Objects.requireNonNull(_trim(source), "projection source is required");
            _value = Objects.requireNonNull(_trim(value), "projection value is required");
        }

        public String key() {
            return _key;
        }

        public String source() {
            return _source;
        }

        public String value() {
            return _value;
        }
    }
}
