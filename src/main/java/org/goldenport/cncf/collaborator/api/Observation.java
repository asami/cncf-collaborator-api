package org.goldenport.cncf.collaborator.api;

import java.util.Map;

/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public interface Observation {
    String code();
    String message();
    Map<String, Object> attributes();
}
