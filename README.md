# AppDynamics Tibco BW Monitoring Extension


## Use Case

TIBCO BusinessWorks™ is a family of next-generation, industry-leading enterprise integration products designed to address the new integration challenges faced when transitioning to a digital business.
The Tibco BW Monitoring Extension executes BW methods using BW hawk microagents and presents them in the AppDynamics Metric Browser.

This extension works only with the standalone machine agent.

**Note : By default, the Machine agent and AppServer agent can only send a fixed number of metrics to the controller. This extension potentially reports thousands of metrics, so to change this limit, please follow the instructions mentioned [here](https://docs.appdynamics.com/display/PRO40/Metrics+Limits).**


## Installation

1. Run 'mvn clean install' from tibco-hawk-monitoring-extension
2. Copy and unzip TibcoHawkMonitor-\<version\>.zip from 'target' directory into \<machine_agent_dir\>/monitors/
3. Edit config.yaml file in TibcoHawkMonitor/conf and provide the required configuration (see Configuration section)
4. All the BW hawk methods are configured in metrics.xml file in TibcoHawkMonitor/conf
5. Replace the jars in lib with the jars from your Tibco BW installation. In BW 5.9 all the jars are available in tibrv folder.
6. Restart the Machine Agent.

## Configuration

### config.yaml

**Note: Please avoid using tab (\t) when editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/).**

| Param | Description | Example |
| ----- | ----- | ----- |
| displayName | Display name for the hawk domain | "Domin 1" |
| hawkDomain | BW Hawk domain from which we are trying to get the stats from | "testDomain" |
| rvService | RV service to use to connect to hawk | "7474" |
| rvNetwork | RV network to use to connect to hawk | ";" |
| rvDaemon | RV daemon to use to connect to hawk | "tcp:7474" |
| emsURL | EMS URL to use to connect to hawk | "tcp://localhost:7222" |
| emsUserName | EMS user to use to connect to hawk | "admin" |
| emsPassword | EMS userpassword to use to connect to hawk | "admin" |
| bwMicroagentNameMatcher | regex matcher to match and autodetect the BW hawk microagents  | ".\*bwengine.\*" |

### metrics.xml

This file contains the methods to execute using BW hawk micro agents and metrics to collect from the method results.

**Below is an example config for monitoring multiple BW  domains:**

~~~
hawkConnection:
   - displayName: "RV Domain"
     hawkDomain: "testDomain"
     # Supported transport types are tibrv and tibems
     transportType: "tibrv"
     # RV transport properties
     rvService: "7474"
     rvNetwork: ";"
     rvDaemon: "tcp:7474"
     # EMS transport properties
     #emsURL: ""
     #emsUserName:
     #emsPassword:
     bwMicroagentNameMatcher: [".*bwengine.*testDomain.*"]
     #This pattern is matched with the BW microagent name and the name extracted from specified groups(i.e string matched in the parentheses) is used as the display name. If no group found or invalid group used we will use full microagent name.
     #Example, for "COM.TIBCO.ADAPTER.bwengine.testDomain.TestNew.Process Archive" using the below pattern, display name would be "testDomain-TestNew-Process Archive"
     bwMicroagentDisplayNamePattern: "COM.TIBCO.ADAPTER.bwengine\\.(.*)\\.(.*)\\.(.*)"
     bwMicroagentDisplayNameRegexGroups: [1, 2, 3]
     bwMicroagentDisplayNameRegexGroupSeparator: "-"
   - displayName: "EMS Domain"
     hawkDomain: "emsdomain"
     # Supported transport types are tibrv and tibems
     transportType: "tibems"
     # RV transport properties
     #rvService: "7474"
     #rvNetwork: ";"
     #rvDaemon: "tcp:7474"
     # EMS transport properties
     emsURL: "tcp://localhost:7222"
     emsUserName: "admin"
     emsPassword:
     bwMicroagentNameMatcher: [".*bwengine.*emsdomain.*"]
     #This pattern is matched with the BW microagent name and the name extracted from specified groups(i.e string matched in the parentheses) is used as the display name. If no group found or invalid group used we will use full microagent name.
     #Example, for "COM.TIBCO.ADAPTER.bwengine.testDomain.TestNew.Process Archive" using the below pattern, display name would be "testDomain-TestNew-Process Archive"
     bwMicroagentDisplayNamePattern: "COM.TIBCO.ADAPTER.bwengine\\.(.*)\\.(.*)\\.(.*)"
     bwMicroagentDisplayNameRegexGroups: [1, 2, 3]
     bwMicroagentDisplayNameRegexGroupSeparator: "-"

# number of concurrent tasks
numberOfThreads: 2

numberOfThreadsPerDomain: 5

taskSchedule:
    numberOfThreads: 1
    taskDelaySeconds: 60

#This will create this metric in all the tiers, under this path
#metricPrefix: "Custom Metrics|Tibco BW|"

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|Tibco BW"
~~~

## Metrics
Metric path is typically: **Application Infrastructure Performance|\<Tier\>|Custom Metrics|Tibco BW|** followed by the individual categories/metrics below:

Metrics provided by this extension are depend on the methods and metrics configured in the metrics.xml. Below is list of methods and metrics they provide.

### GetMemoryUsage

| Metric | Description |
| ----- | ----- |
| TotalBytes | Total number of bytes allocated to the process engine. |
| FreeBytes | Total number of bytes that are not currently in use.  |
| UsedBytes |  Total number of bytes that are currently in use.  |
| PercentUsed | Percentage of total bytes that are in use.  |

### GetProcessCount

| Metric | Description |
| ----- | ----- |
| TotalRunningProcesses | Total number of currently executing process instances.  |


### GetProcessDefinitions

For each process definition following metrics are displayed

| Metric | Description |
| ----- | ----- |
| Created |  Number of process instances created for this process definition. |
| Suspended |  Number of times process instances have been suspended.  |
| Swapped | Number of times process instances have been swapped to disk.  |
| Queued | Number of times process instances have been queued for execution. |
| Aborted | Number of times process instances have been aborted.  |
| Completed | Number of process instances that have been successfully completed. |
| Checkpointed | Number of times process instances have executed a checkpoint. |
| TotalExecution | Total execution time (in milliseconds) for all successfully completed process instances. |
| AverageExecution | Average execution time (in milliseconds) for all successfully completed process instances. |
| TotalElapsed | Total elapsed time (in milliseconds) for all successfully completed process instances.  |
| AverageElapsed | Average elapsed clock time (in milliseconds) for all successfully completed process instances.  |
| MinElapsed | Elapsed clock time (in milliseconds) of the process instance that has completed in the shortest amount of elapsed time.  |
| MaxElapsed |  Elapsed clock time (in milliseconds) of the process instance that has completed in the longest amount of elapsed time. |
| MinExecution | Execution time (in milliseconds) of the process instance that has completed in the shortest amount of execution time. |
| MaxExecution | Execution time (in milliseconds) of the process instance that has completed in the longest amount of execution time.  |
| MostRecentExecutionTime | Execution time (in milliseconds) of the most recently completed process instance.  |
| MostRecentElapsedTime | Elapsed clock time (in milliseconds) of the most recently completed process instance.  |
| TimeSinceLastUpdate | Time (in milliseconds) since the statistics have been updated.  |
| CountSinceReset | Number of process instances that have completed since the last reset of the statistics. |


### GetActivities

For each activity in each process definition following metrics are displayed

| Metric | Description |
| ----- | ----- |
| ExecutionCount |  Number of times the activity has been executed.  |
| ElapsedTime | Total clock time (in milliseconds) used by all executions of this activity. This includes waiting time for Sleep, Call Process, and Wait For... activities.  |
| ExecutionTime | Total clock time (in milliseconds) used by all executions of this activity. This does not include waiting time for Sleep, Call Process, and Wait For... activities.  |
| ErrorCount |  Total number of executions of the activity that have returned an error.  |
| LastReturnCode | Status code returned by most recent execution of this activity. This can be either OK, DEAD, or ERROR.  |
| MinElapsedTime | Elapsed clock time (in milliseconds) of the activity execution that has completed in the shortest amount of elapsed time.  |
| MaxElapsedTime | Elapsed clock time (in milliseconds) of the activity execution that has completed in the longest amount of elapsed time.  |
| MinExecutionTime | Execution time (in milliseconds) of the activity execution that has completed in the shortest amount of execution time. |
| MaxExecutionTime | Execution time (in milliseconds) of the activity execution that has completed in the longest amount of execution time.  |
| TimeSinceLastUpdate | Time (in milliseconds) since the statistics have been updated.  |
| ExecutionCountSinceReset | Number of activity executions that have completed since the last reset of the statistics.  |


## Troubleshooting

1. Verify Machine Agent Data: Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Hardware Metrics
2. config.yml: Validate the file [here] (http://www.yamllint.com/)
3. Tibco HAWK BW Microagents: Please verify that BW hawk micro agents are available using hawk display.
4. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=5000 if there is a metric limit reached error in the logs. If you dont see the expected metrics, this could be the cause.
5. Check Logs: There could be some obvious errors in the machine agent logs. Please take a look.
6. Collect Debug Logs: Edit the file, <MachineAgent>/conf/logging/log4j.xml and update the level of the appender com.appdynamics to debug Let it run for 5-10 minutes and attach the logs to a support ticket


## Workbench

Workbench is a feature by which you can preview the metrics before registering it with the controller. This is useful if you want to fine tune the configurations. Workbench is embedded into the extension jar.

To use the workbench

* Follow all the installation steps
* Start the workbench with the command
~~~
  java -jar /path/to/MachineAgent/monitors/TibcoHawkMonitor/tibco-hawk-monitoring-extension.jar
  This starts an http server at http://host:9090/. This can be accessed from the browser.
~~~
* If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors.
~~~
    #Get the stats
    curl http://localhost:9090/api/stats
    #Get the registered metrics
    curl http://localhost:9090/api/metric-paths
~~~
* You can make the changes to config.yml and validate it from the browser or the API
* Once the configuration is complete, you can kill the workbench and start the Machine Agent


## Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/tibco-hawk-monitoring-extension).

## Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/) community.

## Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).

