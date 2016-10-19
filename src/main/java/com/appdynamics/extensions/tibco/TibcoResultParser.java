package com.appdynamics.extensions.tibco;

import COM.TIBCO.hawk.talon.CompositeData;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MicroAgentException;
import COM.TIBCO.hawk.talon.TabularData;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class TibcoResultParser {

    private static final Logger logger = Logger.getLogger(TibcoResultParser.class);

    public List<String> parseMicroAgentInfoResult(Object methodResult, List<String> bwMicroagentNameMatcher) {
        List<String> microAgentIds = new ArrayList<String>();
        if (methodResult instanceof CompositeData) {
            logger.error("Invalid result for getMicroAgentInfo. Expecting TabularData but found CompositeData");
            throw new RuntimeException("Invalid result for getMicroAgentInfo. Expecting TabularData but found CompositeData");
        } else if (methodResult instanceof TabularData) {
            TabularData tabData = (TabularData) methodResult;

            DataElement[][] allDataElements = tabData.getAllDataElements();
            for (DataElement[] dataElements : allDataElements) {

                microAgentIds.add(String.valueOf(dataElements[0].getValue()));
            }
        } else if (methodResult instanceof MicroAgentException) {
            MicroAgentException me = (MicroAgentException) methodResult;
            logger.error("Method execution returned error", me);
        } else if (methodResult == null) {
            logger.info("Method execution returned empty result");
        } else {
            logger.error("Method execution returned unknown type [" + methodResult + "]");
        }

        logger.debug("Found [" + microAgentIds.size() + "] microagents using Self microagent");
        logger.trace("Found microagent names [" + microAgentIds + "]");
        List<String> filteredList = filter(microAgentIds, bwMicroagentNameMatcher);
        logger.debug("Matched BW microagents size [" + filteredList.size() + "]");
        logger.trace("Matched BW microagents [" + filteredList + "]");
        return filteredList;
    }

    private List<String> filter(List<String> microAgentIds, List<String> bwMicroagentNameMatcher) {
        List<String> filteredList = new ArrayList<String>();
        PatternMatcherPredicate predicate = new PatternMatcherPredicate(bwMicroagentNameMatcher);

        for (String microAgentId : microAgentIds) {
            if (predicate.apply(microAgentId)) {
                filteredList.add(microAgentId);
            }
        }
        return filteredList;
    }

    public List<TibcoMetric> parseResult(Object methodResult, Method method) {

        String basePath = method.getBasePath();

        List<TibcoMetric> tibcoMetrics = new ArrayList<TibcoMetric>();

        if (methodResult instanceof CompositeData) {
            CompositeData compData = (CompositeData) methodResult;
            String replacedBasepath = replaceBasepathWithValues(basePath, compData.getDataElements());

            Metric[] metrics = method.getMetrics();

            for (Metric metric : metrics) {
                String value = compData.getValue(metric.getColumnName()).toString();
                String metricType = metric.getMetricType() == null ? method.getMetricType() : metric.getMetricType();

                TibcoMetric tibcoMetric = new TibcoMetric();
                tibcoMetric.setFullPath(replacedBasepath + metric.getLabel());
                tibcoMetric.setMetricType(metricType);
                tibcoMetric.setValue(new BigDecimal(value));

                tibcoMetrics.add(tibcoMetric);
            }
        } else if (methodResult instanceof TabularData) {
            TabularData tabData = (TabularData) methodResult;

            DataElement[][] allDataElements = tabData.getAllDataElements();
            for (DataElement[] dataElements : allDataElements) {
                String replacedBasepath = replaceBasepathWithValues(basePath, dataElements);

                Map<Metric, Integer> metricVsIndex = buildMetricColumnIndexes(method, tabData.getColumnNames());

                for (Map.Entry<Metric, Integer> entry : metricVsIndex.entrySet()) {

                    Metric metric = entry.getKey();

                    String metricLabel = metric.getLabel();
                    String metricValue = dataElements[entry.getValue().intValue()].getValue().toString();

                    if (metric.hasConverters()) {
                        metricValue = metric.convertValue(metric.getColumnName(), metricValue);
                    }

                    String metricType = metric.getMetricType() == null ? method.getMetricType() : metric.getMetricType();

                    TibcoMetric tibcoMetric = new TibcoMetric();
                    tibcoMetric.setFullPath(replacedBasepath + metricLabel);
                    tibcoMetric.setMetricType(metricType);
                    tibcoMetric.setValue(new BigDecimal(metricValue));

                    tibcoMetrics.add(tibcoMetric);
                }
            }
        } else if (methodResult instanceof MicroAgentException) {
            MicroAgentException me = (MicroAgentException) methodResult;
            logger.error("Method execution returned error", me);
        } else if (methodResult == null) {
            logger.info("Method execution returned empty result");
        } else {
            logger.error("Method execution returned unknown type [" + methodResult + "]");
        }
        return tibcoMetrics;
    }

    private Map<Metric, Integer> buildMetricColumnIndexes(Method method, String[] columnNames) {
        Map<Metric, Integer> metricVsIndex = new HashMap<Metric, Integer>();
        Metric[] metrics = method.getMetrics();
        for (int i = 0; i < columnNames.length; i++) {
            for (Metric metric : metrics) {
                if (columnNames[i].equals(metric.getColumnName())) {
                    metricVsIndex.put(metric, i);
                }
            }
        }
        return metricVsIndex;
    }

    private String replaceBasepathWithValues(String basePath, DataElement[] dataElements) {
        String[] split = basePath.split("\\|");

        StringBuilder sb = new StringBuilder();
        for (String pathElement : split) {
            for (DataElement dataElement : dataElements) {
                if (dataElement.getName().equals(pathElement)) {
                    sb.append(dataElement.getValue()).append("|");
                }
            }
        }
        return sb.toString();
    }
}