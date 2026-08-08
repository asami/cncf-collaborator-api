package org.goldenport.cncf.component.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/*
 * @since   Aug.  8, 2026
 * @version Aug.  8, 2026
 * @author  ASAMI, Tomoharu
 */
class ComponentIdentityMigrationClassifierTest {
    @Test
    void registryAuthorityLoadsExactlyFourDeferredReleasesAndFailsClosed() {
        // Given
        String empty = "{\"schemaVersion\":\""
                + ComponentIdentityMigrationClassifier.SCHEMA_VERSION
                + "\",\"entries\":[]}";
        String wrong = "{\"schemaVersion\":\"wrong\",\"entries\":[]}";
        String reordered = "{\"entries\":["
                + _entry_json("org.simplemodeling.textus.Corpus", "0.1.0",
                        "textus-corpus", "Corpus") + ","
                + _entry_json("org.simplemodeling.textus.Experiment", "0.1.0",
                        "textus-experiment", "Experiment") + ","
                + _entry_json("org.simplemodeling.textus.GeoResolver", "0.2.1",
                        "textus-georesolver", "GeoResolver") + ","
                + _entry_json("org.simplemodeling.textus.Sanpomap", "0.2.1",
                        "textus-sanpomap", "Sanpomap")
                + "],\"schemaVersion\":\""
                + ComponentIdentityMigrationClassifier.SCHEMA_VERSION + "\"}";

        // When
        ComponentIdentityResult<ComponentIdentityMigrationClassifier> loaded =
                ComponentIdentityMigrationClassifier.load();
        ComponentIdentityResult<ComponentIdentityMigrationClassifier> emptyresult =
                ComponentIdentityMigrationClassifier.parseRegistry(empty);
        ComponentIdentityResult<ComponentIdentityMigrationClassifier> wrongresult =
                ComponentIdentityMigrationClassifier.parseRegistry(wrong);
        ComponentIdentityResult<ComponentIdentityMigrationClassifier> reorderedresult =
                ComponentIdentityMigrationClassifier.parseRegistry(reordered);

        // Then
        assertTrue(loaded.isSuccess());
        assertEquals(4, loaded.value().orElseThrow().entries().size());
        assertEquals("org.simplemodeling.textus.Corpus",
                loaded.value().orElseThrow().entries().get(0).componentId().qualifiedName());
        assertTrue(emptyresult.isFailure());
        assertTrue(wrongresult.isFailure());
        assertTrue(reorderedresult.isSuccess());
        assertEquals(4, reorderedresult.requireValue().entries().size());
    }

    @Test
    void classifierSeparatesCanonicalDeferredMigrationAndInventoryStates() {
        // Given
        ComponentIdentityMigrationClassifier classifier =
                ComponentIdentityMigrationClassifier.load().requireValue();

        // When
        ComponentIdentityMigrationDecision canonical = _classify(classifier,
                _canonical("0.6.0-SNAPSHOT", "textus-user-account"));
        ComponentIdentityMigrationDecision disagreement = _classify(classifier,
                _canonical("0.6.0-SNAPSHOT", "wrong-artifact"));
        ComponentIdentityMigrationDecision duplicateevidence = _classify(classifier,
                new ComponentIdentityMigrationRequest(
                        "org.simplemodeling.textus", "UserAccount", null, null,
                        "0.6.0-SNAPSHOT",
                        List.of(
                                new ComponentIdentityMigrationRequest.AuthoredProjection(
                                        "artifact", "project.name", "textus-user-account"),
                                new ComponentIdentityMigrationRequest.AuthoredProjection(
                                        "artifact", "project.identity.artifact", "wrong-artifact"))));
        ComponentIdentityMigrationDecision exact = _classify(classifier,
                _legacy("textus-corpus", "Corpus", "0.1.0"));
        ComponentIdentityMigrationDecision snapshot = _classify(classifier,
                _legacy("textus-user-account", "UserAccount", "0.6.0-SNAPSHOT"));
        ComponentIdentityMigrationDecision advanced = _classify(classifier,
                _legacy("textus-corpus", "Corpus", "0.1.1-SNAPSHOT"));
        ComponentIdentityMigrationDecision advancedWrongLocal = _classify(classifier,
                _legacy("textus-corpus", "WrongCorpus", "0.1.1-SNAPSHOT"));
        ComponentIdentityMigrationDecision advancedMissingLocal = _classify(classifier,
                _legacy("textus-corpus", null, "0.1.1"));
        ComponentIdentityMigrationDecision higher = _classify(classifier,
                _legacy("textus-corpus", "Corpus", "0.1.1"));
        ComponentIdentityMigrationDecision lower = _classify(classifier,
                _legacy("textus-corpus", "Corpus", "0.0.9"));
        ComponentIdentityMigrationDecision malformed = _classify(classifier,
                _legacy("textus-corpus", "Corpus", "broken"));
        ComponentIdentityMigrationDecision incomparable = _classify(classifier,
                _legacy("textus-corpus", "Corpus", "0.1.0-RC1"));
        ComponentIdentityMigrationDecision partial = _classify(classifier,
                new ComponentIdentityMigrationRequest(
                        "org.simplemodeling.textus", null, null, null,
                        "0.6.0-SNAPSHOT", Map.of()));

        // Then
        assertEquals(ComponentIdentityMigrationDecision.Status.CANONICAL, canonical.status());
        assertEquals(ComponentIdentityMigrationDecision.Status.PROJECTION_DISAGREEMENT,
                disagreement.status());
        assertEquals(ComponentIdentityMigrationDecision.Status.PROJECTION_DISAGREEMENT,
                duplicateevidence.status());
        assertTrue(duplicateevidence.reason().contains("project.identity.artifact"));
        assertEquals(ComponentIdentityMigrationDecision.Status.DEFERRED_TO_NEXT_VERSION,
                exact.status());
        assertEquals(ComponentIdentityMigrationDecision.Status.MIGRATION_REQUIRED, snapshot.status());
        assertEquals(ComponentIdentityMigrationDecision.Status.MIGRATION_REQUIRED, advanced.status());
        assertEquals(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                advancedWrongLocal.status());
        assertEquals("local-id-mismatch", advancedWrongLocal.reason());
        assertEquals(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR,
                advancedMissingLocal.status());
        assertEquals("local-id-mismatch", advancedMissingLocal.reason());
        assertEquals(ComponentIdentityMigrationDecision.Status.MIGRATION_REQUIRED, higher.status());
        assertEquals(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR, lower.status());
        assertEquals("lower-release", lower.reason());
        assertEquals(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR, malformed.status());
        assertEquals("malformed-release", malformed.reason());
        assertEquals(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR, incomparable.status());
        assertEquals("incomparable-release", incomparable.reason());
        assertEquals(ComponentIdentityMigrationDecision.Status.INVENTORY_ERROR, partial.status());
        assertEquals("partial-canonical-identity", partial.reason());
    }

    private static ComponentIdentityMigrationRequest _canonical(
            String release, String artifact) {
        return new ComponentIdentityMigrationRequest(
                "org.simplemodeling.textus",
                "UserAccount",
                null,
                null,
                release,
                Map.of(
                        "qualifiedId", "org.simplemodeling.textus.UserAccount",
                        "organization", "org.simplemodeling.textus",
                        "artifact", artifact,
                        "jvmPackage", "org.simplemodeling.textus.useraccount",
                        "generatedClass", "UserAccountComponent",
                        "path", "user-account"));
    }

    private static ComponentIdentityMigrationRequest _legacy(
            String artifact, String localid, String release) {
        return new ComponentIdentityMigrationRequest(
                null, null, artifact, localid, release, Map.of());
    }

    private static ComponentIdentityMigrationDecision _classify(
            ComponentIdentityMigrationClassifier classifier,
            ComponentIdentityMigrationRequest request) {
        return classifier.classify(request).requireValue();
    }

    private static String _entry_json(String componentid, String release,
            String artifact, String localid) {
        return "{\"migrationOwner\":\"" + artifact
                + "\",\"legacyLocalId\":\"" + localid
                + "\",\"legacyArtifact\":\"" + artifact
                + "\",\"release\":\"" + release
                + "\",\"canonicalComponentId\":\"" + componentid + "\"}";
    }
}
