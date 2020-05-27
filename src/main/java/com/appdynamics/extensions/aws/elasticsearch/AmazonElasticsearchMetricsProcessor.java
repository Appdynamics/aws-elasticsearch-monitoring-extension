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

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.appdynamics.extensions.aws.config.Dimension;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessorHelper;
import com.appdynamics.extensions.aws.predicate.MultiDimensionPredicate;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.aws.elasticsearch.util.Constants.NAMESPACE;

/**
 * @author pradeep.nair
 */
public class AmazonElasticsearchMetricsProcessor implements MetricsProcessor {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AmazonElasticsearchMetricsProcessor.class);

    private final List<IncludeMetric> includeMetrics;
    private final List<Dimension> dimensions;

    AmazonElasticsearchMetricsProcessor(List<IncludeMetric> includeMetrics, List<Dimension> dimensions) {
        this.includeMetrics = includeMetrics;
        this.dimensions = dimensions;
    }

    @Override
    public List<AWSMetric> getMetrics(AmazonCloudWatch awsCloudWatch, String accountName,
                                      LongAdder awsRequestsCounter) {
        MultiDimensionPredicate multiDimensionPredicate = new MultiDimensionPredicate(dimensions);
        return MetricsProcessorHelper.getFilteredMetrics(awsCloudWatch, awsRequestsCounter, NAMESPACE,
                includeMetrics, null, multiDimensionPredicate);
    }

    @Override
    public StatisticType getStatisticType(AWSMetric awsMetric) {
        return MetricsProcessorHelper.getStatisticType(awsMetric.getIncludeMetric(), includeMetrics);
    }

    @Override
    public List<Metric> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats) {
        List<Metric> stats = new ArrayList<>();
        Map<String, String> dimensionDisplayNameMap = new HashMap<>();
        for (Dimension dimension : dimensions)
            dimensionDisplayNameMap.put(dimension.getName(), dimension.getDisplayName());
        if (namespaceMetricStats != null) {
            namespaceMetricStats.getAccountMetricStatisticsList().forEach(accountMetricStats -> {
                String accountPrefix = accountMetricStats.getAccountName();
                accountMetricStats.getRegionMetricStatisticsList().forEach(regionMetricStats -> {
                    String regionPrefix = regionMetricStats.getRegion();
                    // reconstruct the metric hierarchy
                    regionMetricStats.getMetricStatisticsList().forEach(metricStats -> {
                        Map<String, String> dimensionValueMap = new HashMap<>();
                        for (com.amazonaws.services.cloudwatch.model.Dimension dimension :
                                metricStats.getMetric().getMetric().getDimensions())
                            dimensionValueMap.put(dimension.getName(), dimension.getValue());
                        StringBuilder partialMetricPath = new StringBuilder();
                        buildMetricPath(partialMetricPath, true, accountPrefix, regionPrefix);
                        arrangeMetricPathHierarchy(partialMetricPath, dimensionDisplayNameMap, dimensionValueMap);
                        String awsMetricName = metricStats.getMetric().getIncludeMetric().getName();
                        buildMetricPath(partialMetricPath, false, awsMetricName);
                        String fullMetricPath = metricStats.getMetricPrefix() + partialMetricPath;
                        if (metricStats.getValue() != null) {
                            Map<String, Object> metricProperties = new HashMap<>();
                            IncludeMetric metricWithConfig = metricStats.getMetric().getIncludeMetric();
                            metricProperties.put("alias", metricWithConfig.getAlias());
                            metricProperties.put("multiplier", metricWithConfig.getMultiplier());
                            metricProperties.put("aggregationType", metricWithConfig.getAggregationType());
                            metricProperties.put("timeRollUpType", metricWithConfig.getTimeRollUpType());
                            metricProperties.put("clusterRollUpType", metricWithConfig.getClusterRollUpType());
                            metricProperties.put("delta", metricWithConfig.isDelta());
                            Metric metric = new Metric(awsMetricName, Double.toString(metricStats.getValue()),
                                    fullMetricPath, metricProperties);
                            stats.add(metric);
                        } else {
                            LOGGER.debug(String.format("Ignoring metric [ %s ] which has value null", fullMetricPath));
                        }
                    });
                });
            });
        }
        return stats;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }


    /**
     * Builds the metric path, if {@code addSeparator} is {@code true} then the metric separator will be inseted
     * after each suffix.
     *
     * @param partialMetricPath Current {@code partialMetricPath}
     * @param addSeparator      Add metric addSeparator between each suffixes if {@code true}
     * @param suffixes          Value to be appended to {@code partialMetricPath}
     */
    private static void buildMetricPath(StringBuilder partialMetricPath, boolean addSeparator, String... suffixes) {
        for (String suffix : suffixes) {
            partialMetricPath.append(suffix);
            if (addSeparator) partialMetricPath.append(METRIC_PATH_SEPARATOR);
        }
    }

    /**
     * Arrange the metric path hierarchy to look like
     * {@code <Account>|<Region>|DomainName|<DomainName_val>|NodeId|<NodeId_val>|ClientId|<ClientId_val>|}
     *
     * @param partialMetricPath       current metric path
     * @param dimensionDisplayNameMap Dimension display name Map
     * @param dimensionValueMap       Dimension value Map
     */
    private static void arrangeMetricPathHierarchy(StringBuilder partialMetricPath,
                                                   Map<String, String> dimensionDisplayNameMap,
                                                   Map<String, String> dimensionValueMap) {
        // Appender for DomainName dimension
        String domainNameDimension = "DomainName";
        String domainNameDisplayName = dimensionDisplayNameMap.getOrDefault(domainNameDimension, domainNameDimension);

        // Appender for ClientId dimension
        String clientIdDimension = "ClientId";
        String clientIdDisplayName = dimensionDisplayNameMap.getOrDefault(clientIdDimension, clientIdDimension);

        // Appender for NodeId dimension
        String nodeIdDimension = "NodeId";
        String nodeIdDisplayName = dimensionDisplayNameMap.getOrDefault(nodeIdDimension, nodeIdDimension);
        // <Account>|<Region>|DomainName|<DomainName_val>|
        buildMetricPath(partialMetricPath, true, domainNameDisplayName, dimensionValueMap.get(domainNameDimension));
        // <Account>|<Region>|DomainName|<DomainName_val>|NodeId|<NodeId_val>|ClientId|<ClientId_val>|
        if (dimensionValueMap.get(clientIdDimension) != null)
            buildMetricPath(partialMetricPath, true, clientIdDisplayName, dimensionValueMap.get(clientIdDimension));
        // <Account>|<Region>|DomainName|<DomainName_val>|NodeId|<NodeId_val>
        if (dimensionValueMap.get(nodeIdDimension) != null)
            buildMetricPath(partialMetricPath, true, nodeIdDisplayName, dimensionValueMap.get(nodeIdDimension));
    }
}
