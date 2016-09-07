package com.appdynamics.extensions.tibco;

import com.appdynamics.extensions.StringUtils;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Satish Muddam
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Metric {

    private static final Logger logger = Logger.getLogger(Metric.class);

    @XmlAttribute
    private String columnName;
    @XmlAttribute
    private String label;
    @XmlAttribute(name = "metric-type")
    private String metricType;
    @XmlAttribute
    private String enabled;
    @XmlElement(name = "converter")
    private MetricConverter[] converters;
    @XmlTransient
    private Map<String, String> converterMap;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    //Dirty method, assuming that these objects do not change.
    public String convertValue(String attr, String value) {
        if (converters != null && converters.length > 0) {
            if (converterMap == null) {
                converterMap = new HashMap<String, String>();
                for (MetricConverter converter : converters) {
                    converterMap.put(converter.getLabel(), converter.getValue());
                }
            }
            String converted = converterMap.get(value);
            if (StringUtils.hasText(converted)) {
                return converted;
            } else if (converterMap.containsKey("$default")) {
                return converterMap.get("$default");
            } else {
                logger.error("For the " + attr + ", the converter map " + converterMap + " has no value for [" + value + "]");
                return value;
            }
        }
        return value;
    }

    public boolean hasConverters() {
        return converters != null && converters.length > 0;
    }
}
