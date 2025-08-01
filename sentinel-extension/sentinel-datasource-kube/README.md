# Sentinel DataSource Kube

Sentinel DataSource Kube provides integration with [Kube](https://kubernetes.io/) so that Kube configmap
can be the dynamic rule data source of Sentinel.

To use Sentinel DataSource Kube, you should add the following dependency:

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-kube</artifactId>
    <version>x.y.z</version>
</dependency>
```

Then you can create an `KubeDataSource` and register to rule managers.
For instance:

```java
ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new KubeDatasource<>(remoteAddress, groupId, dataId,
    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
```