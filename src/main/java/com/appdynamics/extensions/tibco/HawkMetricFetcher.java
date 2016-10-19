package com.appdynamics.extensions.tibco;

import COM.TIBCO.hawk.console.hawkeye.AgentManager;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsole;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsoleFactory;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentData;
import COM.TIBCO.hawk.talon.MicroAgentException;
import COM.TIBCO.hawk.talon.MicroAgentID;
import COM.TIBCO.hawk.utilities.misc.HawkConstants;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Satish Muddam
 */
public class HawkMetricFetcher implements Runnable {

    private static final Logger logger = Logger.getLogger(HawkMetricFetcher.class);

    private static final String SELF_MICROAGENT_NAME = "COM.TIBCO.hawk.microagent.Self";

    private MonitorConfiguration configuration;
    private Map hawkConnection;
    private TibcoResultParser tibcoResultParser;
    private String hawkDomainDisplayName;
    private Method[] methods;
    private Integer numberOfThreadsPerDomain;

    public HawkMetricFetcher(MonitorConfiguration configuration, Map hawkConnection, Method[] methods, Integer numberOfThreadsPerDomain) {
        this.configuration = configuration;
        this.hawkConnection = hawkConnection;
        hawkDomainDisplayName = (String) hawkConnection.get("displayName");
        tibcoResultParser = new TibcoResultParser();
        this.methods = methods;
        this.numberOfThreadsPerDomain = numberOfThreadsPerDomain;
    }

    public void run() {

        try {

            AgentManager agentManager = connect();
            List<MicroAgentID> bwMicroagents = getBWMicroagents(agentManager);

            if (bwMicroagents == null || bwMicroagents.isEmpty()) {
                logger.error("No BW microagents found for the specified patterns. Exiting the process.");
                return;
            }

            List<Method> methodsToExecute = Lists.newArrayList(this.methods);

            for (Method method : methodsToExecute) {
                if (method.isEnabled()) {
                    executeMethod(agentManager, bwMicroagents, method);
                } else {
                    logger.info("Method " + method.getMethodName() + " is not enabled, hence skipping");
                }
            }

        } catch (Exception e) {
            logger.error("Error while collecting metrics from domain [" + hawkDomainDisplayName + "]", e);
        }
    }

    private List<MicroAgentID> getBWMicroagents(AgentManager agentManager) {
        List<String> bwMicroagentNameMatcher = (List) hawkConnection.get("bwMicroagentNameMatcher");

        MicroAgentID[] selfMicroAgentIDs = null;
        try {
            selfMicroAgentIDs = agentManager.getMicroAgentIDs(SELF_MICROAGENT_NAME, 1);
        } catch (MicroAgentException e) {
            logger.error("Error while trying to get microagents from [ " + SELF_MICROAGENT_NAME + " ]", e);
            throw new RuntimeException("Error while trying to get microagents from [ " + SELF_MICROAGENT_NAME + " ]", e);
        }

        if (selfMicroAgentIDs == null || selfMicroAgentIDs.length <= 0) {
            logger.error("Unable to find micro agent [ " + SELF_MICROAGENT_NAME + " ]");
            throw new IllegalArgumentException("Unable to find micro agent [ " + SELF_MICROAGENT_NAME + " ]");
        }

        Object methodResult = executeMethod(agentManager, selfMicroAgentIDs[0], "getMicroAgentInfo", null);
        List<String> bwMicroAgentIdNames = tibcoResultParser.parseMicroAgentInfoResult(methodResult, bwMicroagentNameMatcher);

        List<MicroAgentID> bwMicroAgentIds = new ArrayList<MicroAgentID>();

        for (String bwMicroAgentId : bwMicroAgentIdNames) {
            try {
                MicroAgentID[] microAgentIDs = agentManager.getMicroAgentIDs(bwMicroAgentId);
                bwMicroAgentIds.add(microAgentIDs[0]);
            } catch (MicroAgentException e) {
                logger.error("Unable to get the microagent with name [" + bwMicroAgentId + "]");
            }
        }

        return bwMicroAgentIds;
    }

    private AgentManager connect() {
        String hawkDomain = (String) hawkConnection.get("hawkDomain");
        String rvService = (String) hawkConnection.get("rvService");
        String rvNetwork = (String) hawkConnection.get("rvNetwork");
        String rvDaemon = (String) hawkConnection.get("rvDaemon");

        if (Strings.isNullOrEmpty(hawkDomain) || Strings.isNullOrEmpty(rvService)
                || Strings.isNullOrEmpty(rvNetwork) || Strings.isNullOrEmpty(rvDaemon)) {
            logger.error("Please provide hawkDomain, rvService, rvNetwork, rvDaemon in the config.");
            throw new IllegalArgumentException("Please provide hawkDomain, rvService, rvNetwork, rvDaemon in the config.");
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(HawkConstants.HAWK_TRANSPORT, HawkConstants.HAWK_TRANSPORT_TIBRV);
        result.put(HawkConstants.RV_SERVICE, rvService);
        result.put(HawkConstants.RV_NETWORK, rvNetwork);
        result.put(HawkConstants.RV_DAEMON, rvDaemon);
        result.put(HawkConstants.HAWK_DOMAIN, hawkDomain);

        AgentManager agentManager = null;
        try {
            TIBHawkConsole hawkConsole = TIBHawkConsoleFactory.getInstance().createHawkConsole(result);
            agentManager = hawkConsole.getAgentManager();
            agentManager.initialize();
        } catch (Exception e) {
            logger.error("Exception while connecting to hawk", e);
            throw new RuntimeException("Exception while connecting to hawk", e);
        }
        return agentManager;
    }

    private void executeMethod(final AgentManager agentManager, List<MicroAgentID> bwMicroagents, final Method method) {

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreadsPerDomain);

        for (final MicroAgentID microAgentID : bwMicroagents) {
            executorService.execute(new Runnable() {
                public void run() {
                    logger.debug("Executing in thread " + Thread.currentThread().getName());
                    logger.debug("Executing method [" + method.getMethodName() + "] on microagent [" + microAgentID.getName() + "]");
                    Object methodResult = executeMethod(agentManager, microAgentID, method.getMethodName(), null);
                    logger.trace("Method [" + method.getMethodName() + "] result on microagent [" + microAgentID.getName() + "] is [" + methodResult + "]");
                    printData(method.getMethodName(), null, methodResult, method, microAgentID.getName());
                }
            });
        }
        executorService.shutdown();
    }

    private Object executeMethod(AgentManager agentManager, MicroAgentID microAgentID, String methodName, DataElement[] args) {

        MethodInvocation mi = new MethodInvocation(methodName, args);
        try {
            MicroAgentData m = agentManager.invoke(microAgentID, mi);
            return m.getData();
        } catch (Exception e) {
            logger.error("Error while executing method [ " + methodName + "] on microagent [" + microAgentID.getName() + "]", e);
        }
        return null;
    }

    private void printData(String methodName, String methodDisplayName, Object methodResult, Method method, String agentDisplayName) {

        StringBuilder methodNameWithDisplayName = new StringBuilder(methodName);
        if (methodDisplayName != null && methodDisplayName.length() > 0) {
            methodNameWithDisplayName.append("|").append(methodDisplayName);
        }

        TibcoResultParser tibcoResultParser = new TibcoResultParser();
        List<TibcoMetric> tibcoMetrics = tibcoResultParser.parseResult(methodResult, method);

        logger.debug("Collected " + tibcoMetrics.size() + " metrics for method [" + methodName + "] on microagent [" + agentDisplayName + "]");

        for (TibcoMetric tibcoMetric : tibcoMetrics) {
            configuration.getMetricWriter().printMetric(configuration.getMetricPrefix() + "|" + hawkDomainDisplayName + "|" + agentDisplayName + "|" + methodNameWithDisplayName + "|" + tibcoMetric.getFullPath(), tibcoMetric.getValue(), tibcoMetric.getMetricType());
        }
    }
}