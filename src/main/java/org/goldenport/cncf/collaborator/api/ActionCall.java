package org.goldenport.cncf.collaborator.api;

import java.util.Map;

/*
 * @since   Jan. 27, 2026
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
public interface ActionCall {
    String operationName();
    Map<String, Object> arguments();
}
