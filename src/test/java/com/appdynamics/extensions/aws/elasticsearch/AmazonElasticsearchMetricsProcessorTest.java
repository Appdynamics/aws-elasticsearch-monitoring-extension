/*
 * Copyright (c) 2019 AppDynamics,Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.aws.elasticsearch;

import com.appdynamics.extensions.aws.config.Dimension;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.metrics.Metric;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author pradeep.nair
 */
public class AmazonElasticsearchMetricsProcessorTest {
    private final NamespaceMetricStatistics namespaceMetricStats = new NamespaceMetricStatistics();
    private AmazonElasticsearchMetricsProcessor amazonElasticsearchMetricsProcessor;

    @Before
    public void setup() {
        // Populate dimension with their display name
        // DomainName dimension
        List<Dimension> dimensions = new ArrayList<>();
        Dimension domainDimension = new Dimension();
        domainDimension.setName("DomainName");
        domainDimension.setDisplayName("Domain Name");
        dimensions.add(domainDimension);
        // ClientId dimension
        Dimension clientIdDimension = new Dimension();
        clientIdDimension.setName("ClientId");
        clientIdDimension.setDisplayName("Client Id");
        dimensions.add(clientIdDimension);
        // NodeId dimension
        Dimension nodeIdDimension = new Dimension();
        nodeIdDimension.setName("NodeId");
        nodeIdDimension.setDisplayName("Node Id");
        dimensions.add(nodeIdDimension);

        amazonElasticsearchMetricsProcessor = new AmazonElasticsearchMetricsProcessor(new ArrayList<>(), dimensions);
        createNamespaceMetricStatsForTest();
    }

    private void createNamespaceMetricStatsForTest() {
        AccountMetricStatistics accountStats = new AccountMetricStatistics();
        accountStats.setAccountName("Appd");
        RegionMetricStatistics regionStats = new RegionMetricStatistics();
        regionStats.setRegion("us-west-2");
        IncludeMetric includeMetric = new IncludeMetric();
        includeMetric.setName("testMetric");

        List<com.amazonaws.services.cloudwatch.model.Dimension> dimensions = new ArrayList<>();
        com.amazonaws.services.cloudwatch.model.Dimension domainDimension =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        domainDimension.withName("DomainName").withValue("es-domain");
        dimensions.add(domainDimension);
        com.amazonaws.services.cloudwatch.model.Dimension clientIdDimension =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        clientIdDimension.withName("ClientId").withValue("7654321");
        dimensions.add(clientIdDimension);
        com.amazonaws.services.cloudwatch.model.Dimension nodeIdDimension =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        nodeIdDimension.withName("NodeId").withValue("zxy4389301adgsd");
        dimensions.add(nodeIdDimension);

        com.amazonaws.services.cloudwatch.model.Metric metric = new com.amazonaws.services.cloudwatch.model.Metric();
        metric.setDimensions(dimensions);

        AWSMetric awsMetric = new AWSMetric();
        awsMetric.setIncludeMetric(includeMetric);
        awsMetric.setMetric(metric);

        MetricStatistic metricStatistic = new MetricStatistic();
        metricStatistic.setMetric(awsMetric);
        metricStatistic.setValue(new Random().nextDouble());
        metricStatistic.setUnit("testUnit");
        metricStatistic.setMetricPrefix("Custom Metrics|Amazon Elasticsearch|");

        regionStats.addMetricStatistic(metricStatistic);
        accountStats.add(regionStats);
        namespaceMetricStats.add(accountStats);
    }

    @Test
    public void createMetricStatsMapAndCheckMetricPathHierarchyWithDimensionTest() {
        List<Metric> stats = amazonElasticsearchMetricsProcessor
                .createMetricStatsMapForUpload(namespaceMetricStats);
        Metric metric = stats.get(0);
        String expectedMetricName = "Custom Metrics|Amazon Elasticsearch|Appd|us-west-2|Domain Name|es-domain|Client " +
                "Id|7654321|Node Id|zxy4389301adgsd|testMetric";
        assertNotNull(metric);
        assertThat(metric.getMetricPath(), is(equalTo(expectedMetricName)));
    }
}