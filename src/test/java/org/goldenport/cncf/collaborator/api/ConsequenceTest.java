package org.goldenport.cncf.collaborator.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConsequenceTest {
    @Test
    void successConsequenceExposesValue() {
        Object marker = new Object();
        Consequence consequence = new SuccessConsequence(marker);
        assertTrue(consequence.isSuccess());
        assertSame(marker, consequence.value());
        assertNull(consequence.observation());
    }

    @Test
    void failureConsequenceExposesObservation() {
        Observation observation = new DefaultObservation("code", "details", Map.of("stage", "validate"));
        Consequence consequence = new FailureConsequence(observation);
        assertFalse(consequence.isSuccess());
        assertSame(observation, consequence.observation());
        assertNull(consequence.value());
    }
}
