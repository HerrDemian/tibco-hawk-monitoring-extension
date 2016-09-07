package com.appdynamics.extensions.tibco;


import java.math.BigDecimal;

/**
 * @author Satish Muddam
 */
public class TibcoMetric {

    private String fullPath;
    private BigDecimal value;
    private String metricType;

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
}
