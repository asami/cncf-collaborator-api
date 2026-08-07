package org.goldenport.cncf.component.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ComponentIdentityTest {
    @Nested
    @DisplayName("namespace identity")
    class NamespaceIdentity {
        @Test
        void namespaceParseReturnsExactValidValue() {
            // Given
            String source = "org.simplemodeling.textus";

            // When
            ComponentIdentityResult<ComponentNamespace> result = ComponentNamespace.parse(source);
            ComponentNamespace required = ComponentNamespace.require(source);

            // Then
            assertTrue(result.isSuccess());
            assertFalse(result.isFailure());
            assertEquals(source, result.value().orElseThrow().value());
            assertEquals("textus", result.value().orElseThrow().finalSegment());
            assertTrue(result.error().isEmpty());
            assertEquals(source, required.toString());
        }

        @Test
        void namespaceParseReportsRequiredFormatAndReservedFailuresAsValues() {
            // Given
            String malformedsource = "org..textus";
            String unicodesource = "org.simplémodeling.textus";
            String reservedsource = "org.def.textus";

            // When
            ComponentIdentityResult<ComponentNamespace> missing = ComponentNamespace.parse(null);
            ComponentIdentityResult<ComponentNamespace> malformed = ComponentNamespace.parse(malformedsource);
            ComponentIdentityResult<ComponentNamespace> unicode = ComponentNamespace.parse(unicodesource);
            ComponentIdentityResult<ComponentNamespace> reserved = ComponentNamespace.parse(reservedsource);
            ComponentIdentityResult<ComponentNamespace> using = ComponentNamespace.parse("org.using.textus");
            ComponentIdentityResult<ComponentNamespace> provides = ComponentNamespace.parse("org.provides.textus");
            ComponentIdentityResult<ComponentNamespace> when = ComponentNamespace.parse("org.when.textus");

            // Then
            _assert_error(missing, "component.identity.namespace.required");
            _assert_error(malformed, "component.identity.namespace.segment-format");
            _assert_error(unicode, "component.identity.namespace.segment-format");
            _assert_error(reserved, "component.identity.namespace.segment-reserved");
            _assert_error(using, "component.identity.namespace.segment-reserved");
            _assert_error(provides, "component.identity.namespace.segment-reserved");
            _assert_error(when, "component.identity.namespace.segment-reserved");
        }

        @Test
        void namespaceParseRejectsEveryFrozenJavaAndScalaReservedSegmentAndAdmitsExactControls() {
            // Given
            List<String> reservedsegments = List.of(
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

            // When
            List<ComponentIdentityResult<ComponentNamespace>> reservedresults = reservedsegments.stream()
                    .map(segment -> ComponentNamespace.parse("org." + segment + ".textus"))
                    .toList();
            List<ComponentIdentityResult<ComponentNamespace>> controlresults = reservedsegments.stream()
                    .map(segment -> ComponentNamespace.parse("org." + segment + "x.textus"))
                    .toList();
            ComponentIdentityResult<ComponentNamespace> forsome =
                    ComponentNamespace.parse("org.forsome.textus");
            ComponentIdentityResult<ComponentNamespace> forsomecase =
                    ComponentNamespace.parse("org.forSome.textus");

            // Then
            for (ComponentIdentityResult<ComponentNamespace> result : reservedresults) {
                _assert_error(result, "component.identity.namespace.segment-reserved");
            }
            for (ComponentIdentityResult<ComponentNamespace> result : controlresults) {
                assertTrue(result.isSuccess());
            }
            assertTrue(forsome.isSuccess());
            _assert_error(forsomecase, "component.identity.namespace.segment-format");
        }
    }

    @Nested
    @DisplayName("local ID")
    class LocalId {
        @Test
        void localIdParseReturnsExactValidValueWithoutNormalization() {
            // Given
            String source = "OAuth2Client";

            // When
            ComponentIdentityResult<ComponentLocalId> result = ComponentLocalId.parse(source);
            ComponentLocalId required = ComponentLocalId.require(source);

            // Then
            assertTrue(result.isSuccess());
            assertEquals(source, result.value().orElseThrow().value());
            assertEquals(source, required.toString());
        }

        @Test
        void localIdParseReportsRequiredAndFormatFailuresAsValues() {
            // Given
            String lowercasesource = "userAccount";
            String formattedsource = "User-Account";

            // When
            ComponentIdentityResult<ComponentLocalId> missing = ComponentLocalId.parse(null);
            ComponentIdentityResult<ComponentLocalId> lowercase = ComponentLocalId.parse(lowercasesource);
            ComponentIdentityResult<ComponentLocalId> formatted = ComponentLocalId.parse(formattedsource);
            ComponentIdentityResult<ComponentLocalId> unicode = ComponentLocalId.parse("ÜserAccount");

            // Then
            _assert_error(missing, "component.identity.local-id.required");
            _assert_error(lowercase, "component.identity.local-id.format");
            _assert_error(formatted, "component.identity.local-id.format");
            _assert_error(unicode, "component.identity.local-id.format");
        }
    }

    @Nested
    @DisplayName("qualified component ID")
    class QualifiedComponentId {
        @Test
        void componentIdParseRoundTripsExactQualifiedIdentity() {
            // Given
            String namespace = "org.simplemodeling.textus";
            String localid = "UserAccount";
            String qualifiedid = "org.simplemodeling.textus.UserAccount";

            // When
            ComponentId constructed = ComponentId.of(
                    ComponentNamespace.require(namespace), ComponentLocalId.require(localid));
            ComponentIdentityResult<ComponentId> result =
                    ComponentId.parse(qualifiedid);
            ComponentId required = ComponentId.require(qualifiedid);

            // Then
            assertTrue(result.isSuccess());
            assertEquals(constructed, result.value().orElseThrow());
            assertEquals(qualifiedid, result.value().orElseThrow().qualifiedName());
            assertEquals(constructed, required);
        }

        @Test
        void componentIdParseReportsStructuralAndNestedFailuresWithStableCodes() {
            // Given
            String baresource = "UserAccount";
            String finaldotsource = "org.simplemodeling.textus.";
            String leadingdotsource = ".org.simplemodeling.textus.UserAccount";
            String extradotsource = "org.simplemodeling..textus.UserAccount";
            String invalidnamespacesource = "org.Simplemodeling.textus.UserAccount";
            String invalidlocalidsource = "org.simplemodeling.textus.userAccount";

            // When
            ComponentIdentityResult<ComponentId> bare = ComponentId.parse(baresource);
            ComponentIdentityResult<ComponentId> finaldot = ComponentId.parse(finaldotsource);
            ComponentIdentityResult<ComponentId> leadingdot = ComponentId.parse(leadingdotsource);
            ComponentIdentityResult<ComponentId> extradot =
                    ComponentId.parse(extradotsource);
            ComponentIdentityResult<ComponentId> invalidnamespace =
                    ComponentId.parse(invalidnamespacesource);
            ComponentIdentityResult<ComponentId> invalidlocalid =
                    ComponentId.parse(invalidlocalidsource);

            // Then
            _assert_error(bare, "component.identity.id.qualified");
            _assert_error(finaldot, "component.identity.id.qualified");
            _assert_error(leadingdot, "component.identity.namespace.segment-format");
            _assert_error(extradot, "component.identity.namespace.segment-format");
            _assert_error(invalidnamespace, "component.identity.namespace.segment-format");
            _assert_error(invalidlocalid, "component.identity.local-id.format");
        }
    }

    @Nested
    @DisplayName("identity projections and releases")
    class IdentityProjectionsAndReleases {
        @Test
        void projectionExposesExactQualifiedAndMavenGroupIdentity() {
            // Given
            String qualifiedid = "org.simplemodeling.textus.UserAccount";

            // When
            ComponentId componentid = _require_component_id(qualifiedid);
            ComponentIdentityProjection projection = ComponentIdentityProjection.of(componentid);

            // Then
            assertEquals("org.simplemodeling.textus.UserAccount", projection.qualifiedId());
            assertEquals("org.simplemodeling.textus", projection.mavenGroupId());
            assertEquals(componentid, projection.componentId());
        }

        @Test
        void projectionPreservesTheFrozenWordBoundaryMatrixAcrossDerivedFields() {
            // Given
            String namespace = "org.simplemodeling.textus";

            // When
            List<ComponentIdentityProjection> projections = List.of(
                    ComponentIdentityProjection.of(_require_component_id(namespace + ".UserAccount")),
                    ComponentIdentityProjection.of(_require_component_id(namespace + ".HTTPGateway")),
                    ComponentIdentityProjection.of(_require_component_id(namespace + ".OAuth2Client")),
                    ComponentIdentityProjection.of(_require_component_id(namespace + ".HTTP2Gateway")));

            // Then
            _assert_projection(projections.get(0), namespace, "UserAccount", "user-account", "useraccount");
            _assert_projection(projections.get(1), namespace, "HTTPGateway", "http-gateway", "httpgateway");
            _assert_projection(projections.get(2), namespace, "OAuth2Client", "oauth2-client", "oauth2client");
            _assert_projection(projections.get(3), namespace, "HTTP2Gateway", "http2-gateway", "http2gateway");
        }

        @Test
        void releaseProjectionReturnsSafeFailuresAndRequireConveniences() {
            // Given
            String qualifiedid = "org.simplemodeling.textus.UserAccount";
            List<String> invalidreleases = List.of(
                    "0.6.0/SNAPSHOT", "0.6.0\\\\SNAPSHOT", "0.6.0:SNAPSHOT",
                    "0.6.0 SNAPSHOT", "0.6.0\nSNAPSHOT", ".6.0", "-0.6.0", "_0.6.0", "+0.6.0");
            List<String> invalidscalasuffixes = List.of(
                    "_3", "2..12", ".3", "3.", "3:2", "3/2", "3\\\\2", "3 2", "3\n2");

            // When
            ComponentIdentityProjection projection = ComponentIdentityProjection.of(
                    _require_component_id(qualifiedid));
            ComponentIdentityResult<String> nullrelease = projection.carFilename(null);
            ComponentIdentityResult<String> blankrelease = projection.carFilename(" ");
            ComponentIdentityResult<String> nullscala = projection.mavenCoordinate(null, "0.6.0-SNAPSHOT");
            ComponentIdentityResult<String> blankscala = projection.mavenCoordinate(" ", "0.6.0-SNAPSHOT");
            ComponentIdentityResult<String> blankcoordinate = projection.mavenCoordinate("3", " ");
            List<ComponentIdentityResult<String>> invalidreleaseresults = invalidreleases.stream()
                    .map(projection::carFilename)
                    .toList();
            List<ComponentIdentityResult<String>> invalidscalaresults = invalidscalasuffixes.stream()
                    .map(scalasuffix -> projection.mavenCoordinate(scalasuffix, "0.6.0-SNAPSHOT"))
                    .toList();
            String requiredfilename = projection.requireCarFilename("0.6.0-SNAPSHOT");
            String buildfilename = projection.requireCarFilename("0.6.0+build.7");
            String requiredcoordinate = projection.requireMavenCoordinate("3", "0.6.0-SNAPSHOT");
            String buildcoordinate = projection.requireMavenCoordinate("2.12", "0.6.0+build.7");
            IllegalStateException requiredfailure = assertThrows(
                    IllegalStateException.class, () -> projection.requireCarFilename(""));
            IllegalStateException formatfailure = assertThrows(
                    IllegalStateException.class,
                    () -> projection.requireMavenCoordinate("_3", "0.6.0-SNAPSHOT"));

            // Then
            assertEquals("textus-user-account-0.6.0-SNAPSHOT.car", requiredfilename);
            assertEquals("textus-user-account-0.6.0+build.7.car", buildfilename);
            assertEquals("org.simplemodeling.textus:textus-user-account_3:0.6.0-SNAPSHOT", requiredcoordinate);
            assertEquals("org.simplemodeling.textus:textus-user-account_2.12:0.6.0+build.7", buildcoordinate);
            _assert_error(nullrelease, "component.identity.release.required");
            _assert_error(blankrelease, "component.identity.release.required");
            _assert_error(nullscala, "component.identity.scala-suffix.required");
            _assert_error(blankscala, "component.identity.scala-suffix.required");
            _assert_error(blankcoordinate, "component.identity.release.required");
            for (ComponentIdentityResult<String> result : invalidreleaseresults) {
                _assert_error(result, "component.identity.release.format");
            }
            for (ComponentIdentityResult<String> result : invalidscalaresults) {
                _assert_error(result, "component.identity.scala-suffix.format");
            }
            assertTrue(requiredfailure.getMessage().contains("component.identity.release.required"));
            assertTrue(formatfailure.getMessage().contains("component.identity.scala-suffix.format"));
        }
    }

    @Nested
    @DisplayName("instance identity")
    class InstanceIdentity {
        @Test
        void releaseAndInstanceLabelAreExcludedFromCanonicalComponentId() {
            // Given
            String qualifiedid = "org.simplemodeling.textus.UserAccount";

            // When
            ComponentId componentid = _require_component_id(qualifiedid);
            ComponentIdentityProjection projection = ComponentIdentityProjection.of(componentid);
            ComponentInstanceId development = ComponentInstanceId.require(componentid, "development");
            ComponentInstanceId production = ComponentInstanceId.require(componentid, "production");
            ComponentId required = ComponentId.require(qualifiedid);
            String snapshotfilename = projection.requireCarFilename("0.6.0-SNAPSHOT");
            String laterfilename = projection.requireCarFilename("0.6.1-SNAPSHOT");

            // Then
            assertEquals(componentid, required);
            assertNotEquals(snapshotfilename, laterfilename);
            assertEquals(componentid, development.componentId());
            assertEquals(componentid, production.componentId());
            assertNotEquals(development, production);
        }

        @Test
        void instanceIdRetainsExactValuesAndProvidesMapSetValueSemantics() {
            // Given
            String qualifiedid = "org.simplemodeling.textus.UserAccount";
            String otherqualifiedid = "org.simplemodeling.textus.UserProfile";

            // When
            ComponentId componentid = _require_component_id(qualifiedid);
            ComponentIdentityResult<ComponentInstanceId> defaultresult =
                    ComponentInstanceId.defaultInstance(componentid);
            ComponentIdentityResult<ComponentInstanceId> equaldefaultresult =
                    ComponentInstanceId.of(componentid, ComponentInstanceId.DEFAULT_LABEL);
            ComponentIdentityResult<ComponentInstanceId> nondefaultresult =
                    ComponentInstanceId.of(componentid, "secondary");
            ComponentInstanceId defaultinstance = defaultresult.requireValue();
            ComponentInstanceId equaldefault = equaldefaultresult.requireValue();
            ComponentInstanceId nondefault = nondefaultresult.requireValue();
            ComponentInstanceId otherdefault = ComponentInstanceId.requireDefaultInstance(
                    _require_component_id(otherqualifiedid));
            Set<ComponentInstanceId> instances = new HashSet<>(List.of(defaultinstance, equaldefault, nondefault));

            // Then
            assertEquals(ComponentInstanceId.DEFAULT_LABEL, defaultinstance.label());
            assertEquals(componentid, defaultinstance.componentId());
            assertEquals(defaultinstance, equaldefault);
            assertEquals(defaultinstance.hashCode(), equaldefault.hashCode());
            assertEquals(2, instances.size());
            assertNotEquals(defaultinstance, nondefault);
            assertNotEquals(defaultinstance, otherdefault);
            assertEquals("org.simplemodeling.textus.UserAccount@default", defaultinstance.toString());
        }

        @Test
        void instanceFactoriesReturnTypedFailuresAndRetainExactNonEmptyLabels() {
            // Given
            String qualifiedid = "org.simplemodeling.textus.UserAccount";
            String exactlabel = " production label ";

            // When
            ComponentId componentid = _require_component_id(qualifiedid);
            ComponentIdentityResult<ComponentInstanceId> nullcomponent = ComponentInstanceId.of(null, "default");
            ComponentIdentityResult<ComponentInstanceId> nulllabel = ComponentInstanceId.of(componentid, null);
            ComponentIdentityResult<ComponentInstanceId> emptylabel = ComponentInstanceId.of(componentid, "");
            ComponentIdentityResult<ComponentInstanceId> defaultnullcomponent =
                    ComponentInstanceId.defaultInstance(null);
            ComponentIdentityResult<ComponentInstanceId> exact = ComponentInstanceId.of(componentid, exactlabel);

            // Then
            _assert_error(nullcomponent, "component.identity.instance.component-id.required");
            _assert_error(nulllabel, "component.identity.instance.label.required");
            _assert_error(emptylabel, "component.identity.instance.label.required");
            _assert_error(defaultnullcomponent, "component.identity.instance.component-id.required");
            assertEquals(exactlabel, exact.value().orElseThrow().label());
        }

        @Test
        void instanceRequireConveniencesThrowForInvalidInputsWithStableCodes() {
            // Given
            String qualifiedid = "org.simplemodeling.textus.UserAccount";

            // When
            ComponentId componentid = _require_component_id(qualifiedid);
            IllegalStateException nullcomponent = assertThrows(
                    IllegalStateException.class, () -> ComponentInstanceId.require(null, "default"));
            IllegalStateException emptylabel = assertThrows(
                    IllegalStateException.class, () -> ComponentInstanceId.require(componentid, ""));
            IllegalStateException defaultnullcomponent = assertThrows(
                    IllegalStateException.class, () -> ComponentInstanceId.requireDefaultInstance(null));

            // Then
            assertTrue(nullcomponent.getMessage().contains("component.identity.instance.component-id.required"));
            assertTrue(emptylabel.getMessage().contains("component.identity.instance.label.required"));
            assertTrue(defaultnullcomponent.getMessage().contains(
                    "component.identity.instance.component-id.required"));
        }
    }

    @Nested
    @DisplayName("CAR release coordinates")
    class CarReleaseCoordinates {
        @Test
        void releaseCoordinateProjectsCanonicalCarRepositoryAbi() {
            // Given
            ComponentId componentid = _require_component_id("org.simplemodeling.textus.UserAccount");
            String release = "0.6.0-SNAPSHOT";
            String sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

            // When
            ComponentIdentityResult<ComponentReleaseCoordinate> result =
                    ComponentReleaseCoordinate.create(componentid, release);
            ComponentReleaseCoordinate coordinate = ComponentReleaseCoordinate.require(componentid, release);

            // Then
            assertTrue(result.isSuccess());
            assertEquals(coordinate, result.value().orElseThrow());
            assertEquals(componentid, coordinate.componentId());
            assertEquals(release, coordinate.release());
            assertEquals("org.simplemodeling.textus.UserAccount", coordinate.qualifiedId());
            assertEquals("org.simplemodeling.textus", coordinate.mavenGroupId());
            assertEquals("textus-user-account", coordinate.mavenArtifactId());
            assertEquals("org.simplemodeling.textus.UserAccount:0.6.0-SNAPSHOT",
                    coordinate.dependencyKey());
            assertEquals("org.simplemodeling.textus:textus-user-account:0.6.0-SNAPSHOT",
                    coordinate.mavenReleaseKey());
            assertEquals("org/simplemodeling/textus", coordinate.groupPath());
            assertEquals("textus-user-account-0.6.0-SNAPSHOT.car", coordinate.carFilename());
            assertEquals("org/simplemodeling/textus/textus-user-account/0.6.0-SNAPSHOT/"
                    + "textus-user-account-0.6.0-SNAPSHOT.car", coordinate.carRepositoryRelativePath());
            assertEquals(coordinate.carRepositoryRelativePath(), coordinate.carCacheRelativePath());
            assertEquals("car/org/simplemodeling/textus/textus-user-account.yaml",
                    coordinate.carCatalogRelativePath());
            assertEquals("car:org.simplemodeling.textus:UserAccount", coordinate.carIndexKey());
            assertEquals("org.simplemodeling.textus:textus-user-account:0.6.0-SNAPSHOT@sha256:"
                    + sha256, coordinate.requireIntegrityKey(sha256));
        }

        @Test
        void sameArtifactFilenameInSeparateNamespacesRetainsDistinctRepositoryCoordinates() {
            // Given
            String release = "0.6.0-SNAPSHOT";
            String sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

            // When
            ComponentReleaseCoordinate first = ComponentReleaseCoordinate.require(
                    _require_component_id("org.alpha.textus.Shared"), release);
            ComponentReleaseCoordinate second = ComponentReleaseCoordinate.require(
                    _require_component_id("org.beta.textus.Shared"), release);

            // Then
            assertEquals(first.mavenArtifactId(), second.mavenArtifactId());
            assertEquals(first.carFilename(), second.carFilename());
            assertNotEquals(first.qualifiedId(), second.qualifiedId());
            assertNotEquals(first.dependencyKey(), second.dependencyKey());
            assertNotEquals(first.mavenReleaseKey(), second.mavenReleaseKey());
            assertNotEquals(first.groupPath(), second.groupPath());
            assertNotEquals(first.carRepositoryRelativePath(), second.carRepositoryRelativePath());
            assertNotEquals(first.carCacheRelativePath(), second.carCacheRelativePath());
            assertNotEquals(first.carCatalogRelativePath(), second.carCatalogRelativePath());
            assertNotEquals(first.carIndexKey(), second.carIndexKey());
            assertEquals("org.alpha.textus:textus-shared:0.6.0-SNAPSHOT@sha256:" + sha256,
                    first.requireIntegrityKey(sha256));
            assertEquals("org.beta.textus:textus-shared:0.6.0-SNAPSHOT@sha256:" + sha256,
                    second.requireIntegrityKey(sha256));
            assertNotEquals(first.requireIntegrityKey(sha256), second.requireIntegrityKey(sha256));
        }

        @Test
        void releaseCoordinateReturnsTypedFailuresAndRequireThrowsForInvalidInputs() {
            // Given
            ComponentId componentid = _require_component_id("org.simplemodeling.textus.UserAccount");

            // When
            ComponentIdentityResult<ComponentReleaseCoordinate> nullcomponent =
                    ComponentReleaseCoordinate.create(null, "0.6.0-SNAPSHOT");
            ComponentIdentityResult<ComponentReleaseCoordinate> nullrelease =
                    ComponentReleaseCoordinate.create(componentid, null);
            ComponentIdentityResult<ComponentReleaseCoordinate> blankrelease =
                    ComponentReleaseCoordinate.create(componentid, " ");
            ComponentIdentityResult<ComponentReleaseCoordinate> invalidrelease =
                    ComponentReleaseCoordinate.create(componentid, "0.6.0/SNAPSHOT");
            IllegalStateException nullfailure = assertThrows(IllegalStateException.class,
                    () -> ComponentReleaseCoordinate.require(null, "0.6.0-SNAPSHOT"));
            IllegalStateException blankfailure = assertThrows(IllegalStateException.class,
                    () -> ComponentReleaseCoordinate.require(componentid, ""));
            IllegalStateException formatfailure = assertThrows(IllegalStateException.class,
                    () -> ComponentReleaseCoordinate.require(componentid, "0.6.0/SNAPSHOT"));

            // Then
            _assert_error(nullcomponent, "component.identity.component.required");
            _assert_error(nullrelease, "component.identity.release.required");
            _assert_error(blankrelease, "component.identity.release.required");
            _assert_error(invalidrelease, "component.identity.release.format");
            assertTrue(nullfailure.getMessage().contains("component.identity.component.required"));
            assertTrue(blankfailure.getMessage().contains("component.identity.release.required"));
            assertTrue(formatfailure.getMessage().contains("component.identity.release.format"));
        }

        @Test
        void integrityKeyRequiresBareLowercaseSha256AndCoordinateValueSemanticsAreDeterministic() {
            // Given
            ComponentId componentid = _require_component_id("org.simplemodeling.textus.UserAccount");
            ComponentId separatelyparsed = _require_component_id("org.simplemodeling.textus.UserAccount");
            ComponentReleaseCoordinate coordinate = ComponentReleaseCoordinate.require(componentid,
                    "0.6.0-SNAPSHOT");
            ComponentReleaseCoordinate equal = ComponentReleaseCoordinate.require(separatelyparsed,
                    "0.6.0-SNAPSHOT");
            ComponentReleaseCoordinate later = ComponentReleaseCoordinate.require(componentid,
                    "0.6.1-SNAPSHOT");
            ComponentReleaseCoordinate differentcomponent = ComponentReleaseCoordinate.require(
                    _require_component_id("org.simplemodeling.textus.UserProfile"), "0.6.0-SNAPSHOT");
            String validdigest = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
            List<String> paths = List.of(coordinate.groupPath(), coordinate.carFilename(),
                    coordinate.carRepositoryRelativePath(), coordinate.carCacheRelativePath(),
                    coordinate.carCatalogRelativePath());

            // When
            ComponentIdentityResult<String> nullsha = coordinate.integrityKey(null);
            ComponentIdentityResult<String> emptysha = coordinate.integrityKey("");
            ComponentIdentityResult<String> whitespacesha = coordinate.integrityKey(" \t\n");
            ComponentIdentityResult<String> prefixedsha = coordinate.integrityKey("sha256:" + validdigest);
            ComponentIdentityResult<String> surroundedsha = coordinate.integrityKey(" " + validdigest + " ");
            ComponentIdentityResult<String> uppercasesha = coordinate.integrityKey(validdigest.toUpperCase(
                    java.util.Locale.ROOT));
            ComponentIdentityResult<String> shortsha = coordinate.integrityKey(validdigest.substring(0, 63));
            ComponentIdentityResult<String> longsha = coordinate.integrityKey(validdigest + "0");
            ComponentIdentityResult<String> nonhexsha = coordinate.integrityKey("g" + validdigest.substring(1));
            IllegalStateException emptyfailure = assertThrows(IllegalStateException.class,
                    () -> coordinate.requireIntegrityKey(""));
            IllegalStateException sha256failure = assertThrows(IllegalStateException.class,
                    () -> coordinate.requireIntegrityKey("sha256:" + validdigest));

            // Then
            _assert_error(nullsha, "component.identity.sha256.required");
            _assert_error(emptysha, "component.identity.sha256.required");
            _assert_error(whitespacesha, "component.identity.sha256.required");
            _assert_error(prefixedsha, "component.identity.sha256.format");
            _assert_error(surroundedsha, "component.identity.sha256.format");
            _assert_error(uppercasesha, "component.identity.sha256.format");
            _assert_error(shortsha, "component.identity.sha256.format");
            _assert_error(longsha, "component.identity.sha256.format");
            _assert_error(nonhexsha, "component.identity.sha256.format");
            assertTrue(emptyfailure.getMessage().contains("component.identity.sha256.required"));
            assertTrue(sha256failure.getMessage().contains("component.identity.sha256.format"));
            for (String path : paths) {
                assertFalse(path.contains(".."));
                assertFalse(path.startsWith("/"));
                assertFalse(path.contains("\\"));
            }
            assertEquals(componentid, separatelyparsed);
            assertNotSame(componentid, separatelyparsed);
            assertEquals(coordinate, equal);
            assertEquals(coordinate.hashCode(), equal.hashCode());
            assertNotEquals(coordinate, later);
            assertNotEquals(coordinate, differentcomponent);
            assertEquals(coordinate.mavenReleaseKey(), coordinate.toString());
        }
    }

    @Nested
    @DisplayName("collision admission")
    class CollisionAdmission {
        @Test
        void collisionValidationRejectsSameNamespaceMavenArtifactCollision() {
            // Given
            String firstqualifiedid = "org.simplemodeling.textus.HTTPGateway";
            String secondqualifiedid = "org.simplemodeling.textus.HttpGateway";

            // When
            ComponentId first = _require_component_id(firstqualifiedid);
            ComponentId second = _require_component_id(secondqualifiedid);
            ComponentIdentityResult<List<ComponentId>> result =
                    ComponentIdentityProjection.validateNoScopedCollisions(List.of(first, second));
            ComponentIdentityResult<List<ComponentId>> permuted =
                    ComponentIdentityProjection.validateNoScopedCollisions(List.of(second, first));
            IllegalStateException requiredfailure = assertThrows(IllegalStateException.class, () ->
                    ComponentIdentityProjection.requireNoScopedCollisions(List.of(first, second)));

            // Then
            _assert_error(result, "component.identity.projection.collision");
            assertTrue(result.error().orElseThrow().message().contains("Maven artifact"));
            assertTrue(result.error().orElseThrow().message().contains(first.qualifiedName()));
            assertTrue(result.error().orElseThrow().message().contains(second.qualifiedName()));
            assertEquals(result.error(), permuted.error());
            assertTrue(permuted.error().orElseThrow().message().contains("Maven artifact"));
            assertTrue(requiredfailure.getMessage().contains("component.identity.projection.collision"));
        }

        @Test
        void collisionValidationRejectsJvmOnlyCaseFoldedLocalIdCollision() {
            // Given
            String firstqualifiedid = "org.simplemodeling.textus.A1B";
            String secondqualifiedid = "org.simplemodeling.textus.A1b";

            // When
            ComponentId first = _require_component_id(firstqualifiedid);
            ComponentId second = _require_component_id(secondqualifiedid);
            ComponentIdentityResult<List<ComponentId>> result =
                    ComponentIdentityProjection.validateNoScopedCollisions(List.of(first, second));
            ComponentIdentityProjection firstprojection = ComponentIdentityProjection.of(first);
            ComponentIdentityProjection secondprojection = ComponentIdentityProjection.of(second);

            // Then
            assertEquals("textus-a1-b", firstprojection.mavenArtifactId());
            assertEquals("textus-a1b", secondprojection.mavenArtifactId());
            _assert_error(result, "component.identity.projection.collision");
            assertTrue(result.error().orElseThrow().message().contains("JVM package"));
            assertTrue(result.error().orElseThrow().message().contains(first.qualifiedName()));
            assertTrue(result.error().orElseThrow().message().contains(second.qualifiedName()));
        }

        @Test
        void collisionValidationAdmitsDifferentNamespacesWithEqualHumanFacingFilenames() {
            // Given
            String firstqualifiedid = "org.simplemodeling.textus.HTTPGateway";
            String secondqualifiedid = "io.example.textus.HTTPGateway";

            // When
            ComponentId first = _require_component_id(firstqualifiedid);
            ComponentId second = _require_component_id(secondqualifiedid);
            ComponentIdentityResult<List<ComponentId>> result =
                    ComponentIdentityProjection.validateNoScopedCollisions(List.of(first, second));
            ComponentIdentityProjection firstprojection = ComponentIdentityProjection.of(first);
            ComponentIdentityProjection secondprojection = ComponentIdentityProjection.of(second);
            String firstfilename = firstprojection.requireCarFilename("0.6.0-SNAPSHOT");
            String secondfilename = secondprojection.requireCarFilename("0.6.0-SNAPSHOT");
            UnsupportedOperationException immutablefailure = assertThrows(
                    UnsupportedOperationException.class, () -> result.value().orElseThrow().add(first));

            // Then
            assertTrue(result.isSuccess());
            assertEquals(List.of(second, first), result.value().orElseThrow());
            assertEquals(UnsupportedOperationException.class, immutablefailure.getClass());
            assertEquals(firstfilename, secondfilename);
            assertNotEquals(firstprojection.mavenGroupId() + ":" + firstprojection.mavenArtifactId(),
                    secondprojection.mavenGroupId() + ":" + secondprojection.mavenArtifactId());
            assertNotEquals(firstprojection.jvmPackage(), secondprojection.jvmPackage());
            assertNotEquals(firstprojection.mavenGroupId() + ":" + firstprojection.pathSegment(),
                    secondprojection.mavenGroupId() + ":" + secondprojection.pathSegment());
        }

        @Test
        void collisionValidationReturnsCanonicalImmutableOrderForUnorderedAndPermutedSuccesses() {
            // Given
            String firstqualifiedid = "org.simplemodeling.textus.UserAccount";
            String secondqualifiedid = "io.example.textus.UserProfile";

            // When
            ComponentId first = _require_component_id(firstqualifiedid);
            ComponentId second = _require_component_id(secondqualifiedid);
            Set<ComponentId> unordered = new HashSet<>(List.of(first, second));
            ComponentIdentityResult<List<ComponentId>> unorderedresult =
                    ComponentIdentityProjection.validateNoScopedCollisions(unordered);
            ComponentIdentityResult<List<ComponentId>> permutedresult =
                    ComponentIdentityProjection.validateNoScopedCollisions(List.of(first, second));
            UnsupportedOperationException immutablefailure = assertThrows(
                    UnsupportedOperationException.class,
                    () -> unorderedresult.value().orElseThrow().add(first));

            // Then
            List<ComponentId> expected = List.of(second, first);
            assertEquals(expected, unorderedresult.value().orElseThrow());
            assertEquals(expected, permutedresult.value().orElseThrow());
            assertEquals(unorderedresult.value(), permutedresult.value());
            assertEquals(UnsupportedOperationException.class, immutablefailure.getClass());
        }
    }

    private static ComponentId _require_component_id(String qualifiedid) {
        return ComponentId.require(qualifiedid);
    }

    private static void _assert_error(ComponentIdentityResult<?> result, String code) {
        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertTrue(result.value().isEmpty());
        assertEquals(code, result.error().orElseThrow().code());
    }

    private static void _assert_projection(ComponentIdentityProjection projection,
            String namespace, String localid, String pathsegment, String lowerflatlocalid) {
        assertEquals(namespace + "." + localid, projection.qualifiedId());
        assertEquals(namespace, projection.mavenGroupId());
        assertEquals("textus-" + pathsegment, projection.mavenArtifactId());
        assertEquals(namespace + "." + lowerflatlocalid, projection.jvmPackage());
        assertEquals(localid + "Component", projection.generatedClassName());
        assertEquals(pathsegment, projection.pathSegment());
    }
}
