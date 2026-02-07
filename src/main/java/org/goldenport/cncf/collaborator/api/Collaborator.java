package org.goldenport.cncf.collaborator.api;

/**
 * A minimal entry point for invoking actions and receiving consequences.
 */
/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public interface Collaborator {
    Consequence invoke(ActionCall call);
}
