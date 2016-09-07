package com.appdynamics.extensions.tibco;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Satish Muddam
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Method {

    @XmlAttribute
    private String methodName;
    @XmlAttribute
    private String basePath;
    @XmlAttribute(name = "metric-type")
    private String metricType;
    @XmlAttribute
    private String enabled;
    @XmlAttribute
    private String dependsOn;
    @XmlElement(name = "metric")
    private Metric[] metrics;
    @XmlElement(name = "argument")
    private Argument[] arguments;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public Boolean isEnabled() {
        return Boolean.valueOf(enabled);
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Metric[] getMetrics() {
        return metrics;
    }

    public void setMetrics(Metric[] metrics) {
        this.metrics = metrics;
    }

    public Argument[] getArguments() {
        return arguments;
    }

    public void setArguments(Argument[] arguments) {
        this.arguments = arguments;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Methods {
        @XmlElement(name = "method")
        private Method[] methods;

        public Method[] getMethods() {
            return methods;
        }

        public void setMethods(Method[] methods) {
            this.methods = methods;
        }
    }
}
