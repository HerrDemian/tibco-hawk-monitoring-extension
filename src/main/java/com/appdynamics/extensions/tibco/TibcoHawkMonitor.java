package com.appdynamics.extensions.tibco;


import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author Satish Muddam
 */
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
            conf.setMetricsXml(metricFilePath, Method.Methods.class);

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRICS_XML, MonitorConfiguration.ConfItem.METRIC_PREFIX,
                    MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            if (!initialized) {
                logger.info("Tibco Hawk Monitor is still initializing");
                return;
            }

            Map<String, ?> config = configuration.getConfigYml();

            Method[] methods = ((Method.Methods) configuration.getMetricsXmlConfiguration()).getMethods();

            if (methods == null || methods.length <= 0) {
                logger.error("Methods are not configured in the metrics.xml. Exiting the process");
                return;
            }

            List<Map> hawkConnections = (List<Map>) config.get("hawkConnection");
            Integer numberOfThreadsPerDomain = (Integer) config.get("numberOfThreadsPerDomain");

            for (Map hawkConnection : hawkConnections) {

                HawkMetricFetcher task = new HawkMetricFetcher(configuration, hawkConnection, methods, numberOfThreadsPerDomain);
                configuration.getExecutorService().execute(task);
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
