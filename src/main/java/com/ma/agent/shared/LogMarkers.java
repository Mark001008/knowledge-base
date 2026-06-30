package com.ma.agent.shared;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * 日志 Marker 常量，与 logback-spring.xml 中 OnMarkerEvaluator 对应。
 */
public final class LogMarkers {

    public static final Marker API  = MarkerFactory.getMarker("API");
    public static final Marker BIZ  = MarkerFactory.getMarker("BIZ");
    public static final Marker DATA = MarkerFactory.getMarker("DATA");

    private LogMarkers() {
    }
}
