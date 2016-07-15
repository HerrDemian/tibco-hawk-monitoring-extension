package com.appdynamics.extensions.tibco;

import COM.TIBCO.hawk.console.hawkeye.AgentManager;
import COM.TIBCO.hawk.talon.CompositeData;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentData;
import COM.TIBCO.hawk.talon.MicroAgentException;
import COM.TIBCO.hawk.talon.MicroAgentID;
import COM.TIBCO.hawk.talon.TabularData;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;


public class HawkMetricFetcher implements Runnable {

    private static final Logger logger = Logger.getLogger(HawkMetricFetcher.class);

    private MonitorConfiguration configuration;
    private AgentManager agentManager;
    private Map hawkMicroAgent;
    private Stat[] stats;
    private CountDownLatch doneSignal;

    public HawkMetricFetcher(MonitorConfiguration configuration, AgentManager agentManager, Map hawkMicroAgent, Stat[] stats, CountDownLatch doneSignal) {
        this.configuration = configuration;
        this.agentManager = agentManager;
        this.hawkMicroAgent = hawkMicroAgent;
        this.stats = stats;
        this.doneSignal = doneSignal;
    }

    public void run() {

        try {
            String agentDisplayName = (String) hawkMicroAgent.get("displayName");
            String agentName = (String) hawkMicroAgent.get("agentName");

            Map<String, Stat> nameVsStat = buildStatsMap();

            MicroAgentID[] microAgentIDs = null;
            try {
                microAgentIDs = agentManager.getMicroAgentIDs(agentName, 1);
            } catch (MicroAgentException e) {
                logger.error("Error while trying to get the microagent [" + agentName + "]", e);
                throw new RuntimeException("Error while trying to get the microagent [" + agentName + "]", e);
            }

            if (microAgentIDs == null || microAgentIDs.length <= 0) {
                logger.error("Unable to find micro agent [" + agentName + "]");
                throw new IllegalArgumentException("Unable to find micro agent [" + agentName + "]");
            }

            List<Map> methods = (List<Map>) hawkMicroAgent.get("methods");

            boolean isBWProcessStatisticsCollectionEnabled = false;

            for (Map method : methods) {

                String methodName = (String) method.get("methodName");
                Map<String, String> methodArgs = (Map) method.get("arguments");

                DataElement[] args = null;

                if (methodArgs != null && methodArgs.size() > 0) {
                    Set<Map.Entry<String, String>> entries = methodArgs.entrySet();

                    args = new DataElement[entries.size()];

                    int i = 0;
                    for (Map.Entry<String, String> methodArg : entries) {
                        args[i++] = new DataElement(methodArg.getKey(), methodArg.getValue());
                    }
                }

                Stat stat = nameVsStat.get(methodName);
                if (stat == null) {
                    logger.error("Could not find method [" + methodName + "] entry in the metrics.xml.");
                    continue;
                }

                if (!isBWProcessStatisticsCollectionEnabled && args != null) {
                    boolean result = enableBWProcessStatisticsCollection(microAgentIDs[0], args);
                    if (result) {
                        isBWProcessStatisticsCollectionEnabled = true;
                    }
                }

                if (isBWProcessStatisticsCollectionEnabled || args == null) {
                    Object methodResult = executeMethod(microAgentIDs[0], methodName, args);
                    printData(methodName, methodResult, stat, agentDisplayName);
                } else {
                    logger.error("Not executing method [" + methodName + "] as extension is not able to enable BWProcessStatisticsCollection for [" + microAgentIDs[0] + "] with args [" + Arrays.toString(args) + "]");
                }
            }
        } finally {
            doneSignal.countDown();
        }
    }

    private boolean enableBWProcessStatisticsCollection(MicroAgentID microAgentID, DataElement[] args) {

        Object response = executeMethod(microAgentID, "EnableBWProcessStatisticsCollection", args);

        if (response == null) {
            return true;
        }

        if (response instanceof MicroAgentException) {
            MicroAgentException exc = (MicroAgentException) response;
            if (exc.getMessage().contains("Instrumentation is already enabled")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Stat> buildStatsMap() {
        Map<String, Stat> nameVsStat = new HashMap<String, Stat>();
        for (Stat stat : stats) {
            nameVsStat.put(stat.getMethodName(), stat);
        }
        return nameVsStat;
    }

    private Object executeMethod(MicroAgentID microAgentID, String methodName, DataElement[] args) {

        MethodInvocation mi = new MethodInvocation(methodName, args);
        try {
            MicroAgentData m = agentManager.invoke(microAgentID, mi);
            return m.getData();
        } catch (Exception e) {
            logger.error("Error while executing method [ " + methodName + "]", e);
        }
        return null;
    }

    private void printData(String methodName, Object methodResult, Stat stat, String agentDisplayName) {

        String basePath = stat.getBasePath();

        if (methodResult instanceof CompositeData) {
            CompositeData compData = (CompositeData) methodResult;
            String replacedBasepath = replaceBasepathWithValues(basePath, compData.getDataElements());

            Metric[] metrics = stat.getMetrics();

            for (Metric metric : metrics) {
                String value = compData.getValue(metric.getColumnName()).toString();
                String metricType = metric.getMetricType() == null ? stat.getMetricType() : metric.getMetricType();
                configuration.getMetricWriter().printMetric(configuration.getMetricPrefix() + "|" + agentDisplayName + "|" + methodName + "|" + replacedBasepath + metric.getLabel(), new BigDecimal(value), metricType);
            }
        } else if (methodResult instanceof TabularData) {
            TabularData tabData = (TabularData) methodResult;

            DataElement[][] allDataElements = tabData.getAllDataElements();
            for (DataElement[] dataElements : allDataElements) {
                String replacedBasepath = replaceBasepathWithValues(basePath, dataElements);

                Map<Metric, Integer> metricVsIndex = buildMetricColumnIndexes(stat, tabData.getColumnNames());

                for (Map.Entry<Metric, Integer> entry : metricVsIndex.entrySet()) {

                    Metric metric = entry.getKey();

                    String metricLabel = metric.getLabel();
                    String metricValue = dataElements[entry.getValue().intValue()].getValue().toString();

                    if (metric.hasConverters()) {
                        metricValue = metric.convertValue(metric.getColumnName(), metricValue);
                    }

                    String metricType = metric.getMetricType() == null ? stat.getMetricType() : metric.getMetricType();

                    configuration.getMetricWriter().printMetric(configuration.getMetricPrefix() + "|" + agentDisplayName + "|" + methodName + "|" + replacedBasepath + metricLabel, new BigDecimal(metricValue), metricType);
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
    }

    private Map<Metric, Integer> buildMetricColumnIndexes(Stat stat, String[] columnNames) {
        Map<Metric, Integer> metricVsIndex = new HashMap<Metric, Integer>();
        Metric[] metrics = stat.getMetrics();
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
