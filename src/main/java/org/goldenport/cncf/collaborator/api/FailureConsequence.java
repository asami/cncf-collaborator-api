package org.goldenport.cncf.collaborator.api;

import java.util.Objects;

/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public final class FailureConsequence implements Consequence {
    private final Observation observation;

    public FailureConsequence(Observation observation) {
        this.observation = Objects.requireNonNull(observation, "observation is required");
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    public Observation observation() {
        return observation;
    }
}
