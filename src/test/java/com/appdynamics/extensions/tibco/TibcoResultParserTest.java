package com.appdynamics.extensions.tibco;

import COM.TIBCO.hawk.talon.CompositeData;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.TabularData;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TibcoResultParserTest {

    @Test
    public void testCompositeDataResult() {

        TibcoResultParser tibcoResultParser = new TibcoResultParser();

        CompositeData compositeDataResult = getCompositeData();
        Method method = getStat();
        List<TibcoMetric> tibcoMetrics = tibcoResultParser.parseResult(compositeDataResult, method);

        Assert.assertEquals(2, tibcoMetrics.size());

        TibcoMetric tibcoMetric1 = tibcoMetrics.get(0);
        Assert.assertEquals("MyMetric|Metric1", tibcoMetric1.getFullPath());
        Assert.assertEquals(10, tibcoMetric1.getValue().intValue());

        TibcoMetric tibcoMetric2 = tibcoMetrics.get(1);
        Assert.assertEquals("MyMetric|Metric2", tibcoMetric2.getFullPath());
        Assert.assertEquals(20, tibcoMetric2.getValue().intValue());
    }

    @Test
    public void testTabularDataResult() {

        TibcoResultParser tibcoResultParser = new TibcoResultParser();

        TabularData tabularDataResult = getTabularData();
        Method method = getStat();
        List<TibcoMetric> tibcoMetrics = tibcoResultParser.parseResult(tabularDataResult, method);

        Assert.assertEquals(2, tibcoMetrics.size());

        for (TibcoMetric tibcoMetric : tibcoMetrics) {
            if (tibcoMetric.getFullPath().contains("Metric1")) {
                Assert.assertEquals(10, tibcoMetric.getValue().intValue());
            } else if (tibcoMetric.getFullPath().contains("Metric2")) {
                Assert.assertEquals(20, tibcoMetric.getValue().intValue());
            }
        }
    }

    private TabularData getTabularData() {

        String[] columnNames = new String[]{"Id", "Path", "Metric1", "Metric2"};
        String[] indexNames = new String[]{"Id"};
        Object[][] data = new Object[1][];
        data[0] = new Object[]{1, "MyMetric", "10", "20"};
        TabularData tabularData = new TabularData(columnNames, indexNames, data);

        return tabularData;
    }

    private Method getStat() {
        Method method = new Method();
        method.setBasePath("Path|");
        method.setMetricType("OBS.CUR.COL");

        Metric metric1 = new Metric();
        metric1.setColumnName("Metric1");
        metric1.setLabel("Metric1");

        Metric metric2 = new Metric();
        metric2.setColumnName("Metric2");
        metric2.setLabel("Metric2");

        method.setMetrics(new Metric[]{metric1, metric2});
        return method;
    }

    private CompositeData getCompositeData() {
        DataElement path = new DataElement("Path", "MyMetric");
        DataElement d1 = new DataElement("Metric1", 10);
        DataElement d2 = new DataElement("Metric2", 20);
        return new CompositeData(new DataElement[]{path, d1, d2});
    }
}
