# AWS Elasticsearch Monitoring Extension
## Use Case
Captures AWS Elasticsearch statistics from Amazon CloudWatch and displays them in the AppDynamics Metric Browser.
## Prerequisites
1. Please give the following permissions to the account being used to with the extension.
    ```
    cloudwatch:ListMetrics
    cloudwatch:GetMetricStatistics
    ```
2. In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/latest/Standalone+Machine+Agents) or [SIM Agent](https://docs.appdynamics.com/display/latest/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).
3. The extension needs to be able to connect to AWS CloudWatch in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product using access key and secret key, or have an agent running on EC2 instance, which you can use with instance profile.
## Installation
1. Run 'mvn clean install' from `aws-redshift-monitoring-extension`
2. Copy and unzip `AWSElasticsearchMonitor-<version>.zip` from `target` directory into `<machine_agent_dir>/monitors/`.<br/>Please place the extension in the <b>"monitors"</b> directory of your Machine Agent installation directory. Do not place the extension in the <b>"extensions"</b> directory of your Machine Agent installation directory.
3. Edit config.yml file in AWSElasticsearchMonitor and provide the required configuration (see Configuration section)
4. Restart the Machine Agent.
## Configuration
In order to use the extension, you need to update the config.yml file that is present in the extension folder. The following is a step-by-step explanation of the configurable fields that are present in the `config.yml` file.

 Note: Please avoid using tab (\t) when editing yaml files. Please copy all the contents of the config.yml file and go to [Yaml Validator](http://www.yamllint.com/) . On reaching the website, paste the contents and press the “Go” button on the bottom left.
 If you get a valid output, that means your formatting is correct and you may move on to the next step.

1. The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
2. Provide __accessKey__(required) and __secretKey__(required) of your account(s), also provide __displayAccountName__(any name that represents your account) and
   __regions__(required). If you are running this extension inside an EC2 instance which has __IAM profile__ configured then you don't have to configure __accessKey__ and  __secretKey__ values, extension will use __IAM profile__ to authenticate. You can provide multiple accounts and regions as below -
   ```
   accounts:
     - awsAccessKey: "XXXXXXXX1"
       awsSecretKey: "XXXXXXXXXX1"
       displayAccountName: "TestAccount_1"
       regions: ["us-east-1","us-west-1","us-west-2"]

     - awsAccessKey: "XXXXXXXX2"
       awsSecretKey: "XXXXXXXXXX2"
       displayAccountName: "TestAccount_2"
       regions: ["eu-central-1","eu-west-1"]
   ```
3. If you want to encrypt the __awsAccessKey__ and __awsSecretKey__ then follow the "Credentials Encryption" section and provide the encrypted values in __awsAccessKey__ and __awsSecretKey__. Configure `enableDecryption` of `credentialsDecryptionConfig` to `true` and provide the encryption key in `encryptionKey`. For example,
   ```
   #Encryption key for Encrypted password.
   credentialsDecryptionConfig:
       enableDecryption: "true"
       encryptionKey: "XXXXXXXX"
   ```
4. Provide all valid proxy information if you use it. If not, leave this section as is.
    ```
   proxyConfig:
     host:
     port:
     username:
     password:
    ```
5. To report metrics only from specific dimension values, configure the `dimension` section as below -
    ```
    dimensions:
      - name: "DomainName"
        displayName: "Domain"
        values: [""]
      - name: "ClientId"
        displayName: "Client Id"
        values: [""]
      - name: "NodeId"
        displayName: "Node Id"
        values: [""]
    ```
   If these fields are left empty `[]`, the metrics which require that dimension will not be reported. In order to monitor everything under a dimension, you can simply use `.*` to pull everything from your AWS Environment.
6.  Configure the metrics section. For configuring the metrics, the following properties can be used:

    |Property|Default value|Possible values|Description|
    |---|---|---|---|
    |alias|metric name|Any string|The substitute name to be used in the metric browser instead of metric name.|
    |statType|"ave"|"ave", "sum", "min", "max"|AWS configured values as returned by API|
    |aggregationType|"AVERAGE"|"AVERAGE", "SUM", "OBSERVATION"|[Aggregation qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)|
    |timeRollUpType|"AVERAGE"| "AVERAGE", "SUM", "CURRENT"|[Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
    |clusterRollUpType|"INDIVIDUAL"|"INDIVIDUAL", "COLLECTIVE"|[Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
    |multiplier|1|Any number|Value with which the metric needs to be multiplied.                                                            |
    |convert|null|Any key value map|Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1|
    |delta|false|true, false|If enabled, gives the delta values of metrics instead of actual values.|
    
    For example,
    ```
    - name: "ClusterStatus.green"
      alias: "Cluster Status|Green"
      statType: "max"
      delta: false
      multiplier: 1
      aggregationType: "OBSERVATION"
      timeRollUpType: "AVERAGE"
      clusterRollUpType: "INDIVIDUAL"
    ```  
    __All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.__
7. CloudWatch metrics are delivered on a best-effort basis. This means that the delivery of metrics is not guaranteed to be on-time.
  There may be a case where the metric is updated in CloudWatch much later than when it was processed, with an associated delay. There is a possibility that the extension does not capture the metric, which is the reason there is a time window. The time window allows
  the metric to be updated in CloudWatch before the extension collects it.
    ```
    metricsTimeRange:
      startTimeInMinsBeforeNow: 10
      endTimeInMinsBeforeNow: 0
    ```
8. This field is set as per the defaults suggested by AWS. You can change this if your limit is different.
    ```
    getMetricStatisticsRateLimit: 400
    ```
9. The maximum number of retry attempts for failed requests that can be re-tried.
    ```
    maxErrorRetrySize: 3
    ```
## Metrics
* The AWS Elasticsearch Extension provides AWS Elasticsearch performance metrics as listed[here](https://docs.aws.amazon.com/elasticsearch-service/latest/developerguide/es-managedomains.html#es-managedomains-cloudwatchmetrics).
## Credentials Encryption
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)
## Troubleshooting
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.
## Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information. Please provide the following in order for us to assist you better.
1. Stop the running machine agent.
2. Delete all existing logs under `<MachineAgent>/logs`.
3. Please enable debug logging by editing the file `<MachineAgent>/conf/logging/log4j.xml`. Change the level value of the following `<logger>` elements to debug.
    ```
   <logger name="com.singularity">
   <logger name="com.appdynamics">
   ```
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory `<MachineAgent>/logs/*`.
5. Attach the zipped `<MachineAgent>/conf/*` directory here.
6. Attach the zipped `<MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith` directory here.

For any support related questions, you can also contact [help@appdynamics.com](mailto:help@appdynamics.com).
## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/aws-redshift-monitoring-extension).
## Version
|Name|Version|
|---|---|
|Extension Version|1.0.0|
|Controller Compatibility|4.4 or Later|
|Last Update|May 16, 2019|
|List of Changes|[Change Log](https://github.com/Appdynamics/aws-elasticsearch-monitoring-extension/blob/master/CHANGELOG.md)|