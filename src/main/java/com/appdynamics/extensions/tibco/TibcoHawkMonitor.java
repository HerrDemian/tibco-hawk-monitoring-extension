package com.appdynamics.extensions.tibco;


import COM.TIBCO.hawk.console.hawkeye.AgentManager;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsole;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsoleFactory;
import COM.TIBCO.hawk.utilities.misc.HawkConstants;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TibcoHawkMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|Tibco|";

    private static final Logger logger = Logger.getLogger(TibcoHawkMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_ARG = "metric-file";

    private boolean initialized;
    private MonitorConfiguration configuration;


    public TibcoHawkMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    private static String getImplementationVersion() {
        return TibcoHawkMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        logger.info("Starting the Tibco Hawk Monitoring task.");

        Thread thread = Thread.currentThread();
        ClassLoader originalCl = thread.getContextClassLoader();
        thread.setContextClassLoader(AManagedMonitor.class.getClassLoader());

        try {
            if (!initialized) {
                initialize(args);
            }
            configuration.executeTask();

            logger.info("Finished Tibco Hawk monitor execution");
            return new TaskOutput("Finished Tibco Hawk monitor execution");
        } catch (Exception e) {
            logger.error("Failed to execute the Tibco Hawk monitoring task", e);
            throw new TaskExecutionException("Failed to execute the Tibco Hawk monitoring task" + e);
        } finally {
            thread.setContextClassLoader(originalCl);
        }
    }

    private void initialize(Map<String, String> argsMap) {
        if (!initialized) {
            final String configFilePath = argsMap.get(CONFIG_ARG);
            final String metricFilePath = argsMap.get(METRIC_ARG);

            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);
            conf.setMetricsXml(metricFilePath, Stat.Stats.class);

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRICS_XML, MonitorConfiguration.ConfItem.METRIC_PREFIX,
                    MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            if (!initialized) {
                logger.info("SQL Broker Monitor is still initializing");
                return;
            }

            Map<String, ?> config = configuration.getConfigYml();

            String hawkDomain = (String) config.get("hawkDomain");
            String rvService = (String) config.get("rvService");
            String rvNetwork = (String) config.get("rvNetwork");
            String rvDaemon = (String) config.get("rvDaemon");

            if (Strings.isNullOrEmpty(hawkDomain) || Strings.isNullOrEmpty(rvService)
                    || Strings.isNullOrEmpty(rvNetwork) || Strings.isNullOrEmpty(rvDaemon)) {
                logger.error("Please provide hawkDomain, rvService, rvNetwork, rvDaemon in the config.");
                throw new IllegalArgumentException("Please provide hawkDomain, rvService, rvNetwork, rvDaemon in the config.");
            }

            Map<String, Object> result = new HashMap<String, Object>();
            result = new HashMap<String, Object>();
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

            List<Map> hawkMicroAgents = (List) config.get("hawkMicroAgents");

            Stat[] stats = ((Stat.Stats) configuration.getMetricsXmlConfiguration()).getStats();

            if (hawkMicroAgents != null && !hawkMicroAgents.isEmpty()) {

                CountDownLatch doneSignal = new CountDownLatch(hawkMicroAgents.size());

                for (Map hawkMicroAgent : hawkMicroAgents) {
                    HawkMetricFetcher task = new HawkMetricFetcher(configuration, agentManager, hawkMicroAgent, stats, doneSignal);
                    configuration.getExecutorService().execute(task);
                }

                try {
                    doneSignal.await();
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for task completion", e);
                }

                agentManager.shutdown();

            } else {
                logger.error("There are no hawkMicroAgents configured");
            }
        }
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        logger.getRootLogger().addAppender(ca);

        final TibcoHawkMonitor monitor = new TibcoHawkMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "F:\\AppDynamics\\extensions\\tibco-hawk-monitoring-extension\\src\\main\\resources\\conf\\config.yaml");
        taskArgs.put("metric-file", "F:\\AppDynamics\\extensions\\tibco-hawk-monitoring-extension\\src\\main\\resources\\conf\\metrics.xml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 60, TimeUnit.SECONDS);
    }

}
