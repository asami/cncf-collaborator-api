package org.goldenport.cncf.component.identity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Single registry-backed authority for Component identity migration decisions.
 *
 * @since   Aug.  8, 2026
 * @version Aug.  8, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentIdentityMigrationClassifier {
    public static final String RESOURCE_PATH =
            "META-INF/cncf/component-identity-deferred-release-registry.json";
    public static final String SCHEMA_VERSION =
            "cncf.component-identity-deferred-release-registry.v1";

    private static final Pattern _numeric_release_pattern =
            Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)");
    private static final Pattern _qualified_release_pattern =
            Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+-.+");
    private static final Set<String> _projection_keys = Set.of(
            "qualifiedId", "organization", "artifact", "jvmPackage",
            "generatedClass", "path");
    private static final Set<String> _registry_fields = Set.of("schemaVersion", "entries");
    private static final Set<String> _entry_fields = Set.of(
            "canonicalComponentId", "release", "legacyArtifact", "legacyLocalId",
            "migrationOwner");

    private final List<RegistryEntry> _entries;

    private ComponentIdentityMigrationClassifier(List<RegistryEntry> entries) {
        _entries = List.copyOf(entries);
    }

    public static ComponentIdentityResult<ComponentIdentityMigrationClassifier> load() {
        return load(ComponentIdentityMigrationClassifier.class.getClassLoader());
    }

    public static ComponentIdentityResult<ComponentIdentityMigrationClassifier> load(
            ClassLoader loader) {
        if (loader == null) {
            return _failure("component.identity.migration.registry.loader-required",
                    "registry ClassLoader is required");
        }
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                return _failure("component.identity.migration.registry.resource-missing",
                        "registry resource is missing: " + RESOURCE_PATH);
            }
            return parseRegistry(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException error) {
            return _failure("component.identity.migration.registry.read-failed",
                    "registry resource could not be read: " + _message(error));
        }
    }

    public static ComponentIdentityResult<ComponentIdentityMigrationClassifier> parseRegistry(
            String text) {
        if (text == null) {
            return _failure("component.identity.migration.registry.text-required",
                    "registry text is required");
        }
        Object decoded;
        try {
            decoded = new JsonParser(text).parse();
        } catch (IllegalArgumentException error) {
            return _failure("component.identity.migration.registry.json-invalid", error.getMessage());
        }
        if (!(decoded instanceof Map<?, ?>)) {
            return _failure("component.identity.migration.registry.shape-invalid",
                    "registry must be a JSON object");
        }
        Map<?, ?> registry = (Map<?, ?>) decoded;
        if (!registry.keySet().equals(_registry_fields)) {
            return _failure("component.identity.migration.registry.shape-invalid",
                    "registry must contain exactly schemaVersion and entries");
        }
        Object schemavalue = registry.get("schemaVersion");
        if (!(schemavalue instanceof String)) {
            return _failure("component.identity.migration.registry.schema-version",
                    "schemaVersion must be a string");
        }
        if (!SCHEMA_VERSION.equals(schemavalue)) {
            return _failure("component.identity.migration.registry.schema-version",
                    "expected " + SCHEMA_VERSION + " but was " + schemavalue);
        }
        Object entryvalue = registry.get("entries");
        if (!(entryvalue instanceof List<?>)) {
            return _failure("component.identity.migration.registry.shape-invalid",
                    "entries must be an array");
        }
        List<?> values = (List<?>) entryvalue;
        if (values.size() != 4) {
            return _failure("component.identity.migration.registry.entry-count",
                    "expected 4 entries but was " + values.size());
        }
        List<RegistryEntry> entries = new ArrayList<>();
        Set<String> componentids = new LinkedHashSet<>();
        Set<String> artifacts = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            Object rawentry = values.get(index);
            if (!(rawentry instanceof Map<?, ?>)) {
                return _failure("component.identity.migration.registry.entry-shape",
                        "entry " + index + " must be an object");
            }
            Map<?, ?> entry = (Map<?, ?>) rawentry;
            if (!entry.keySet().equals(_entry_fields) ||
                    entry.values().stream().anyMatch(x -> !(x instanceof String))) {
                return _failure("component.identity.migration.registry.entry-shape",
                        "entry " + index + " must contain exactly the five string fields");
            }
            String componentname = (String) entry.get("canonicalComponentId");
            ComponentIdentityResult<ComponentId> componentresult =
                    ComponentId.parse(componentname);
            if (componentresult.isFailure()) {
                return _failure("component.identity.migration.registry.component-id",
                        componentresult.error().orElseThrow().message());
            }
            ComponentId componentid = componentresult.value().orElseThrow();
            String release = (String) entry.get("release");
            String artifact = (String) entry.get("legacyArtifact");
            String localid = (String) entry.get("legacyLocalId");
            String owner = (String) entry.get("migrationOwner");
            ComponentIdentityResult<ComponentReleaseCoordinate> coordinate =
                    ComponentReleaseCoordinate.create(componentid, release);
            if (coordinate.isFailure()) {
                return _failure("component.identity.migration.registry.release-coordinate",
                        coordinate.error().orElseThrow().message());
            }
            if (!componentid.localId().value().equals(localid)) {
                return _failure("component.identity.migration.registry.local-id-disagreement",
                        "expected " + componentid.localId().value() + " but was " + localid);
            }
            if (!_non_empty(artifact) || !_non_empty(owner)) {
                return _failure("component.identity.migration.registry.non-empty-field",
                        "legacyArtifact and migrationOwner must be non-empty");
            }
            if (!componentids.add(componentid.qualifiedName())) {
                return _failure("component.identity.migration.registry.duplicate-component-id",
                        componentid.qualifiedName());
            }
            if (!artifacts.add(artifact)) {
                return _failure("component.identity.migration.registry.duplicate-artifact", artifact);
            }
            entries.add(new RegistryEntry(componentid, release, artifact, localid, owner));
        }
        entries.sort(Comparator.comparing(x -> x.componentId().qualifiedName()));
        return ComponentIdentityResult.success(new ComponentIdentityMigrationClassifier(entries));
    }

    public List<RegistryEntry> entries() {
        return _entries;
    }

    public ComponentIdentityResult<ComponentIdentityMigrationDecision> classify(
            ComponentIdentityMigrationRequest request) {
        if (request == null) {
            return _failure("component.identity.migration.request-required",
                    "migration request is required");
        }
        boolean hasnamespace = request.namespace().isPresent();
        boolean haslocalid = request.localId().isPresent();
        if (hasnamespace != haslocalid) {
            return ComponentIdentityResult.success(_decision(
                    ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                    "partial-canonical-identity", request, null, null));
        }
        if (hasnamespace) {
            ComponentIdentityResult<ComponentNamespace> namespace =
                    ComponentNamespace.parse(request.namespace().orElseThrow());
            if (namespace.isFailure()) {
                return _decision_failure(namespace.error().orElseThrow());
            }
            ComponentIdentityResult<ComponentLocalId> localid =
                    ComponentLocalId.parse(request.localId().orElseThrow());
            if (localid.isFailure()) {
                return _decision_failure(localid.error().orElseThrow());
            }
            ComponentId componentid = ComponentId.of(
                    namespace.value().orElseThrow(), localid.value().orElseThrow());
            ComponentIdentityProjection projection = ComponentIdentityProjection.of(componentid);
            Optional<String> disagreement = _projection_disagreement(
                    request.authoredProjectionEvidence(), projection);
            if (disagreement.isPresent()) {
                return ComponentIdentityResult.success(_decision(
                        ComponentIdentityMigrationDecision.Status.PROJECTION_DISAGREEMENT,
                        disagreement.orElseThrow(), request, _entry(componentid).orElse(null), projection));
            }
            if (request.release().isEmpty()) {
                return ComponentIdentityResult.success(_decision(
                        ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                        "release-missing", request, _entry(componentid).orElse(null), projection));
            }
            ComponentIdentityResult<ComponentReleaseCoordinate> coordinate =
                    ComponentReleaseCoordinate.create(componentid, request.release().orElseThrow());
            if (coordinate.isFailure()) {
                return _decision_failure(coordinate.error().orElseThrow());
            }
            return ComponentIdentityResult.success(_decision(
                    ComponentIdentityMigrationDecision.Status.CANONICAL,
                    "canonical-identity", request, _entry(componentid).orElse(null), projection));
        }
        return ComponentIdentityResult.success(_classify_legacy(request));
    }

    private ComponentIdentityMigrationDecision _classify_legacy(
            ComponentIdentityMigrationRequest request) {
        String artifact = request.legacyArtifact().orElse(null);
        String localid = request.legacyLocalId().orElse(null);
        String release = request.release().orElse(null);
        RegistryEntry entry = _entries.stream()
                .filter(x -> x.legacyArtifact().equals(artifact))
                .findFirst().orElse(null);
        if (entry != null) {
            if (release == null) {
                return _decision(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                        "release-missing", request, entry, entry.projection());
            }
            if (!entry.legacyLocalId().equals(localid)) {
                return _decision(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                        "local-id-mismatch", request, entry, entry.projection());
            }
            if (release.equals(entry.release())) {
                return _decision(ComponentIdentityMigrationDecision.Status.DEFERRED_TO_NEXT_VERSION,
                        "exact-deferred-release", request, entry, entry.projection());
            }
            if (_is_snapshot(release)) {
                return _decision(ComponentIdentityMigrationDecision.Status.MIGRATION_REQUIRED,
                        "snapshot-release", request, entry, entry.projection());
            }
            Optional<int[]> expected = _numeric_release(entry.release());
            Optional<int[]> actual = _numeric_release(release);
            if (expected.isPresent() && actual.isPresent()) {
                int comparison = _compare(actual.orElseThrow(), expected.orElseThrow());
                if (comparison > 0) {
                    return _decision(ComponentIdentityMigrationDecision.Status.MIGRATION_REQUIRED,
                            "greater-stable-release", request, entry, entry.projection());
                }
                if (comparison < 0) {
                    return _decision(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                            "lower-release", request, entry, entry.projection());
                }
            }
            return _decision(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                    _qualified_release_pattern.matcher(release).matches()
                            ? "incomparable-release" : "malformed-release",
                    request, entry, entry.projection());
        }
        Optional<RegistryEntry> partial = _entries.stream()
                .filter(x -> x.legacyLocalId().equals(localid))
                .findFirst();
        if (partial.isPresent()) {
            return _decision(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                    "partial-registry-match", request, partial.orElseThrow(),
                    partial.orElseThrow().projection());
        }
        if (release == null) {
            return _decision(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                    "release-missing", request, null, null);
        }
        if (_is_snapshot(release)) {
            return _decision(ComponentIdentityMigrationDecision.Status.MIGRATION_REQUIRED,
                    "snapshot-release", request, null, null);
        }
        return _decision(ComponentIdentityMigrationDecision.Status.STRICT_LEGACY,
                "unregistered-stable-release", request, null, null);
    }

    private Optional<RegistryEntry> _entry(ComponentId componentid) {
        return _entries.stream().filter(x -> x.componentId().equals(componentid)).findFirst();
    }

    private static Optional<String> _projection_disagreement(
            List<ComponentIdentityMigrationRequest.AuthoredProjection> authoredprojections,
            ComponentIdentityProjection projection) {
        for (ComponentIdentityMigrationRequest.AuthoredProjection evidence : authoredprojections) {
            if (!_projection_keys.contains(evidence.key())) {
                return Optional.of("unknown-projection:" + evidence.key()
                        + ":source=" + evidence.source());
            }
        }
        Map<String, String> expected = Map.of(
                "qualifiedId", projection.qualifiedId(),
                "organization", projection.mavenGroupId(),
                "artifact", projection.mavenArtifactId(),
                "jvmPackage", projection.jvmPackage(),
                "generatedClass", projection.generatedClassName(),
                "path", projection.pathSegment());
        return authoredprojections.stream()
                .sorted(Comparator.comparing(ComponentIdentityMigrationRequest.AuthoredProjection::key)
                        .thenComparing(ComponentIdentityMigrationRequest.AuthoredProjection::source))
                .filter(x -> !Objects.equals(expected.get(x.key()), x.value()))
                .map(x -> "projection-disagreement:" + x.key()
                        + ":source=" + x.source()
                        + ":expected=" + expected.get(x.key()) + ":actual=" + x.value())
                .findFirst();
    }

    private static ComponentIdentityMigrationDecision _decision(
            ComponentIdentityMigrationDecision.Status status, String reason,
            ComponentIdentityMigrationRequest request, RegistryEntry entry,
            ComponentIdentityProjection projection) {
        return new ComponentIdentityMigrationDecision(status, reason,
                request.release().orElse(null), entry, projection);
    }

    private static ComponentIdentityResult<ComponentIdentityMigrationDecision> _decision_failure(
            ComponentIdentityResult.Error error) {
        return ComponentIdentityResult.failure(error);
    }

    private static boolean _is_snapshot(String value) {
        return value.endsWith("-SNAPSHOT");
    }

    private static Optional<int[]> _numeric_release(String value) {
        Matcher matcher = _numeric_release_pattern.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new int[] {
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            });
        } catch (NumberFormatException error) {
            return Optional.empty();
        }
    }

    private static int _compare(int[] left, int[] right) {
        for (int index = 0; index < left.length; index++) {
            int comparison = Integer.compare(left[index], right[index]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static boolean _non_empty(String value) {
        return value != null && !value.isEmpty() && value.equals(value.trim());
    }

    private static String _message(Exception error) {
        return error.getMessage() == null ? error.getClass().getName() : error.getMessage();
    }

    private static <T> ComponentIdentityResult<T> _failure(String code, String message) {
        return ComponentIdentityResult.failure(new ComponentIdentityResult.Error(code, message));
    }

    private static final class JsonParser {
        private final String _text;
        private int _index;

        private JsonParser(String text) {
            _text = text;
        }

        public Object parse() {
            _skip_whitespace();
            Object result = _parse_value();
            _skip_whitespace();
            if (_index != _text.length()) {
                throw _error("unexpected trailing content");
            }
            return result;
        }

        private Object _parse_value() {
            if (_index >= _text.length()) {
                throw _error("unexpected end of input");
            }
            char current = _text.charAt(_index);
            if (current == '{') {
                return _parse_object();
            }
            if (current == '[') {
                return _parse_array();
            }
            if (current == '"') {
                return _parse_string();
            }
            throw _error("only objects, arrays, and strings are allowed");
        }

        private Map<String, Object> _parse_object() {
            _expect('{');
            _skip_whitespace();
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            if (_consume('}')) {
                return result;
            }
            while (true) {
                _skip_whitespace();
                String key = _parse_string();
                _skip_whitespace();
                _expect(':');
                _skip_whitespace();
                Object previous = result.putIfAbsent(key, _parse_value());
                if (previous != null) {
                    throw _error("duplicate object field: " + key);
                }
                _skip_whitespace();
                if (_consume('}')) {
                    return result;
                }
                _expect(',');
            }
        }

        private List<Object> _parse_array() {
            _expect('[');
            _skip_whitespace();
            List<Object> result = new ArrayList<>();
            if (_consume(']')) {
                return result;
            }
            while (true) {
                _skip_whitespace();
                result.add(_parse_value());
                _skip_whitespace();
                if (_consume(']')) {
                    return result;
                }
                _expect(',');
            }
        }

        private String _parse_string() {
            _expect('"');
            StringBuilder result = new StringBuilder();
            while (_index < _text.length()) {
                char current = _text.charAt(_index++);
                if (current == '"') {
                    return result.toString();
                }
                if (current == '\\') {
                    if (_index >= _text.length()) {
                        throw _error("unterminated escape sequence");
                    }
                    char escaped = _text.charAt(_index++);
                    switch (escaped) {
                        case '"': result.append('"'); break;
                        case '\\': result.append('\\'); break;
                        case '/': result.append('/'); break;
                        case 'b': result.append('\b'); break;
                        case 'f': result.append('\f'); break;
                        case 'n': result.append('\n'); break;
                        case 'r': result.append('\r'); break;
                        case 't': result.append('\t'); break;
                        case 'u': result.append(_parse_unicode()); break;
                        default: throw _error("invalid escape sequence: \\" + escaped);
                    }
                } else if (current < 0x20) {
                    throw _error("unescaped control character in string");
                } else {
                    result.append(current);
                }
            }
            throw _error("unterminated JSON string");
        }

        private char _parse_unicode() {
            if (_index + 4 > _text.length()) {
                throw _error("incomplete unicode escape");
            }
            String value = _text.substring(_index, _index + 4);
            _index += 4;
            try {
                return (char) Integer.parseInt(value, 16);
            } catch (NumberFormatException error) {
                throw _error("invalid unicode escape: " + value);
            }
        }

        private void _skip_whitespace() {
            while (_index < _text.length() && Character.isWhitespace(_text.charAt(_index))) {
                _index++;
            }
        }

        private boolean _consume(char expected) {
            if (_index < _text.length() && _text.charAt(_index) == expected) {
                _index++;
                return true;
            }
            return false;
        }

        private void _expect(char expected) {
            if (!_consume(expected)) {
                throw _error("expected '" + expected + "'");
            }
        }

        private IllegalArgumentException _error(String message) {
            return new IllegalArgumentException(message + " at character " + _index);
        }
    }

    public static final class RegistryEntry {
        private final ComponentId _component_id;
        private final String _release;
        private final String _legacy_artifact;
        private final String _legacy_local_id;
        private final String _migration_owner;
        private final ComponentIdentityProjection _projection;

        private RegistryEntry(ComponentId componentid, String release,
                String legacyartifact, String legacylocalid, String migrationowner) {
            _component_id = componentid;
            _release = release;
            _legacy_artifact = legacyartifact;
            _legacy_local_id = legacylocalid;
            _migration_owner = migrationowner;
            _projection = ComponentIdentityProjection.of(componentid);
        }

        public ComponentId componentId() {
            return _component_id;
        }

        public String release() {
            return _release;
        }

        public String legacyArtifact() {
            return _legacy_artifact;
        }

        public String legacyLocalId() {
            return _legacy_local_id;
        }

        public String migrationOwner() {
            return _migration_owner;
        }

        public ComponentIdentityProjection projection() {
            return _projection;
        }
    }
}
