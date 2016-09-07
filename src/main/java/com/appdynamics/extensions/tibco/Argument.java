package com.appdynamics.extensions.tibco;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author Satish Muddam
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Argument {

    @XmlAttribute
    private String name;
    @XmlAttribute
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
