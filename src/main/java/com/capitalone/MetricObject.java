package com.capitalone;

import org.apache.commons.lang3.StringUtils;
import org.appdynamics.appdrestapi.RESTAccess;
import org.appdynamics.appdrestapi.data.Application;
import org.appdynamics.appdrestapi.data.MetricData;
import org.appdynamics.appdrestapi.data.PolicyViolation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by PXD338 on 6/29/2016.
 */
public class MetricObject {

    private final int NUM_MINUTES = 600; //14 days
    private final double DEFAULT_VALUE = -1.0;
    private final String METRIC_FILEPATH = "src\\main\\java\\com\\capitalone\\metrics.txt";
    Map<String, Double> metricDataMap;
    //Map<String, Double> violationSeverityMap;
    private String appName = "NA";
    private int appID = -1;

    public Map<String, Double> getMetricDataMap() {
        return metricDataMap;
    }

    private RESTAccess getAccess() {
        final String controller = "appdyn-hqa-c01";
        final String port = "80";
        //TODO: DON"T COMMIT THESE!!
        final String user = "INSERTHERE";
        final String passwd = "INSERTHERE";
        final String account = "customer1";
        final boolean useSSL = false;

        return new RESTAccess(controller, port, useSSL, user, passwd, account);
    }

    public MetricObject initialize(String appIdentifier) throws IOException, IllegalAccessException {

        RESTAccess access = getAccess();

        // retrieve appName/ID
        if (StringUtils.isNumeric(appIdentifier))
            setAppName(access.getApplications().getApplications(), appIdentifier);
        else
            setAppID(access.getApplications().getApplications(), appIdentifier);

        buildMetricDataMap(access);

        //populateMetricFields(access);

        //ExHealthRule errorRule = access.getRESTHealthRuleObjExportSingle(appName, "Business Transaction error rate is much higher than normal").getHealthRules().get(0);

        //ExHealthRule performanceRule = access.getRESTHealthRuleObjExportSingle(appName, "Business Transaction response time is much higher than normal").getHealthRules().get(0);


        // TODO: To tell shaun tomorrow morning:
        // We have access to the data (critical, warning, etc.). But to find it for each of the three circles,
        // We need to do some calculations

       /* PrintStream console = System.out;
        File file = new File("out.txt");
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        System.setOut(ps);

        /// testing
        System.out.println(access.getConfigurationItems(appName).toString());

        System.setOut(console);
*/
        return this;
    }


    private void buildMetricDataMap(RESTAccess access) throws IOException, IllegalAccessException {
        FileReader reader = new FileReader(METRIC_FILEPATH);
        BufferedReader bufferedReader = new BufferedReader(reader);

        metricDataMap = new HashMap<String, Double>();

        String line;
        while ((line = bufferedReader.readLine()) != null)
            metricDataMap.put(line, DEFAULT_VALUE);

        reader.close();

        //populate fields
        populateMetricFields(access);
    }

    private void buildViolationSeverityMap(RESTAccess access, long start, long end) throws IOException {


        //populateMetricFields(access);
        //  ExHealthRule errorRule = access.getRESTHealthRuleObjExportSingle(appName, "Business Transaction error rate is much higher than normal").getHealthRules().get(0);
        //ExHealthRule performanceRule = access.getRESTHealthRuleObjExportSingle(appName, "Business Transaction response time is much higher than normal").getHealthRules().get(0);

        //violationSeverityMap = new HashMap<String, Double>();

        metricDataMap.put("Error Rate Severity", 0.0);
        metricDataMap.put("Response Time Severity", 0.0);
        ArrayList<PolicyViolation> violations = access.getHealthRuleViolations(appName, start, end).getPolicyViolations();
        for (PolicyViolation violation : violations) {

            double currErrorRateSeverity = metricDataMap.get("Error Rate Severity");
            double currResponseTimeSeverity = metricDataMap.get("Response Time Severity");

            // If both are already critical, it's pointless to continue
            if (currErrorRateSeverity == 2.0 && currResponseTimeSeverity == 2.0)
                return;

            double severity = violation.getSeverity().equals("CRITICAL") ? 2.0 : 1.0;

            if (violation.getName().equals("Business Transaction error rate is much higher than normal"))
                metricDataMap.replace("Error Rate Severity", Math.max(currErrorRateSeverity, severity));
            else if (violation.getName().equals("Business Transaction response time is much higher than normal"))
                metricDataMap.replace("Response Time Severity", Math.max(currResponseTimeSeverity, severity));
        }


        //errors
        //response time
        //calls

       /* Double errorPerMinWarnVal = Double.parseDouble(errorRule.getWarning().getPolicyCondition().getCondition1().getConditionValue());
        Double errorPerMinCritVal = Double.parseDouble(errorRule.getCritical().getPolicyCondition().getCondition1().getConditionValue());

        Double avgResponseTimeWarnVal = Double.parseDouble(performanceRule.getWarning().getPolicyCondition().getCondition1().getConditionValue());
        Double avgResponseTimeCritVal = Double.parseDouble(performanceRule.getCritical().getPolicyCondition().getCondition1().getConditionValue());

        Double callsPerMinWarnVal = Double.parseDouble(performanceRule.getWarning().getPolicyCondition().getCondition2().getConditionValue());
        Double callsPerMinCritVal = Double.parseDouble(performanceRule.getCritical().getPolicyCondition().getCondition2().getConditionValue());


        violationSeverityMap.put("Errors Per Min Sev", 0.0);
        violationSeverityMap.put("Average Response Time Sev", 0.0);
        violationSeverityMap.put("Calls Per Min Sev", 0.0);
*/
    }

    private void setAppName(ArrayList<Application> apps, String appIdentifier) {

        appID = Integer.valueOf(appIdentifier);

        if (apps == null) {
            System.out.println("Something went wrong because getting applications should be easy!");
            System.exit(1);
        }

        // iterate through array of applications to find the one with matching ID
        for (Application app : apps) {
            if (app.getId() == appID) {
                // extract the name so that we can pull value from appdynamics
                appName = app.getName();
                return;
            }
        }

        System.out.println("Could not find application with ID: " + appID + ".");
        System.exit(1);

    }

    private void setAppID(ArrayList<Application> apps, String appIdentifier) {


        appName = appIdentifier;


        if (apps == null) {
            System.out.println("Something went wrong because getting applications should be easy!");
            System.exit(1);
        }

        for (Application app : apps) {
            if (app.getName() == appName) {
                appID = app.getId();
                return;
            }
        }

        System.out.println("Could not find application with Name: " + appName + ".");
        System.exit(1);

    }

    private void populateMetricFields(RESTAccess access) throws IllegalAccessException, IOException {

        //set boundaries. 2 weeks (20160 minutes), in this case.
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        cal.add(Calendar.MINUTE, -NUM_MINUTES);
        long start = cal.getTimeInMillis();

        //metrics that need to be calculated (some "totals", "percents", etc. aren't provided by appdynamics)
        ArrayList<String> unknownMetrics = new ArrayList<>();

        //contains the names of the metrics requested by user
        for (Map.Entry<String, Double> entry : metricDataMap.entrySet()) {
            String metricName = entry.getKey();

            double metricValue;
            // uses appdynamics api to obtain value. If it returns -1, it isn't a valid metric name--we have to calculate it.
            // "createPath" allows for generic code (e.g. "Total Calls" -> "Overall Application Performance|Total Calls"
            if ((metricValue = getMetricValue(createPath(metricName), access, start, end)) == -1) {
                unknownMetrics.add(metricName);
                continue;
            }

            entry.setValue(metricValue);
        }

        //individually handle atypical possibilities (e.g. "Total Errors", "Node Health Percent", etc.)
        for (String metricName : unknownMetrics)
            metricDataMap.replace(metricName, generateMetricValue(metricName, access, start, end));


        buildViolationSeverityMap(access, start, end);
        testInit();
    }

    private double getMetricValue(String metricPath, RESTAccess access, long start, long end) throws IllegalAccessException {

        // generic call to appdynamics api to retrieve metric value
        ArrayList<MetricData> metricDataArr = access.getRESTGenericMetricQuery(appName, metricPath, start, end, true).getMetric_data();

        // if resulting array is empty, the metric doesn't exist--we have to calculate
        if (metricDataArr.size() > 0)
            return metricDataArr.get(0).getSingleValue().getValue();
        return -1;
    }

    private double generateMetricValue(String metricName, RESTAccess access, long start, long end) throws IllegalAccessException {

        // we have "Errors per Minute", for example. Manipulating the names gives us a generic way to handle all totals
        if (metricName.contains("Total"))
            return NUM_MINUTES * metricDataMap.get(totalToPerMinute(metricName));

        // must pull all of the nodes and all of the health violations.
        // 100 - (Num Violations / Num Nodes) = Node Health Percent
        if (metricName.equals("Node Health Percent"))
            return getNodeHealthPercent(access, start, end);

        // must pull all of the transactions and all of the health violations.
        // 100 - (Num Violations / Num Transactions) = Business Health Percent
        if (metricName.equals("Business Health Percent"))
            return getBusinessHealthPercent(access, start, end);

        return -1;
    }

    // Don't need anymore?
    private double getBusinessHealthPercent(RESTAccess access, long start, long end) {
        return -1.0;
    }

    private double getNodeHealthPercent(RESTAccess access, long start, long end) {

        //get # of violations, divide by # of nodes
        int numNodes = (access.getNodesForApplication(appName).getNodes()).size();
        int numViolations = (access.getHealthRuleViolations(appName, start, end)).getPolicyViolations().size();

        return 100.0 - (numViolations / numNodes);

    }

    private String totalToPerMinute(String currField) {
        return currField.replace("Total ", "") + " per Minute";
    }

    private String createPath(String currMember) {

        // "Open", "Close" part is deprecated. Needed when using reflection. Probably not anymore.
        return "Overall Application Performance|" + currMember.replace("OPEN", "(").replace("CLOSE", ")");
    }

    private void testInit() throws IllegalAccessException {
        metricDataMap.forEach((k, v) -> System.out.println(k + ": " + v));
    }


}
