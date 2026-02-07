package org.goldenport.cncf.collaborator.api;

/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public final class SuccessConsequence implements Consequence {
    private final Object value;

    public SuccessConsequence(Object value) {
        this.value = value;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public Observation observation() {
        return null;
    }
}
