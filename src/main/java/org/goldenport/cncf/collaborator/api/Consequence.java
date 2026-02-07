package org.goldenport.cncf.collaborator.api;

/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public interface Consequence {
    boolean isSuccess();
    Object value();
    Observation observation();
}
