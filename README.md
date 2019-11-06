# servlet-monitor

A Servlet filter to add basic but very useful [Prometheus](https://prometheus.io) metrics for your app.

## Metrics

The only exposed metrics (for now) are the following:

```
request_seconds_bucket{type,status, method, addr, le}
request_seconds_count{type, status, method, addr}
request_seconds_sum{type, status, method, addr}
response_size_bytes{type, status, method, addr} 
dependency_up{name}
```

Where, for a specific request, `type` tells which request protocol was used (e.g. `grpc` or `http`), `status` registers the response HTTP status, `method` registers the request method and `addr` registers the requested endpoint address.

In detail:

1. The `request_seconds_bucket` metric defines the histogram of how many requests are falling into the well defined buckets represented by the label `le`;

2. The `request_seconds_count` is a counter that counts the overall number of requests with those exact label occurrences;

3. The `request_seconds_sum` is a counter that counts the overall sum of how long the requests with those exact label occurrences are taking;

4. The `response_size_bytes` is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the `content-length` response header. If there is no such header, the value exposed as metric will be zero;

5. Finally, `dependency_up` is a metric to register weather a specific dependency is up (1) or down (0). The label `name` registers the dependency name;

## How to

### Importing dependency

Import the following dependency to your project:

#### Maven

```xml
<dependency>
    <groupId>br.com.labbs</groupId>
    <artifactId>servlet-monitor</artifactId>
    <version>${servlet-monitor-version}</version>
</dependency>
```
**Please use the latest version:** 

[![Released Version](https://img.shields.io/maven-central/v/br.com.labbs/servlet-monitor.svg?maxAge=2000)](https://search.maven.org/search?q=br.com.labbs)

### Collecting metrics

The collector filter can be programmatically added to `javax.servlet.ServletContext` or initialized via `web.xml` file.

#### web.xml

You just need to place the code below in your `web.xml` file.

The number of buckets is optionally overridable, and can be configured by passing a comma-separated string of doubles as the `buckets` init parameter. 
The `buckets` default value is `0.1, 0.3, 1.5, 10.5`.
 
```xml
<filter>
    <filter-name>metricsFilter</filter-name>
    <filter-class>MetricsFilterbr.com.labbs.monitor.filter.MetricsFilter</filter-class>
    <init-param>
        <param-name>buckets</param-name>
        <param-value>0.01,0.05,0.1,0.5,1,2.5,5,7.5,10</param-value>
    </init-param>
</filter>
<!-- This must be the first <filter-mapping> in the web.xml file so that you can get
the most accurate measurement of latency and response size. -->
<filter-mapping>
    <filter-name>metricsFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```
**This must be the first `<filter-mapping>` in the `web.xml` file so that you can get the most accurate measurement of latency and response size.**

### Exporting metrics

As well as the metrics filter, the class `MetricsServlet` can be also programmatically added to `javax.servlet.ServletContext` or initialized via `web.xml` file.

#### web.xml

Place the code below in your `web.xml` file to expose the metrics at the `/metrics` path.

```xml
<servlet>
    <servlet-name>Metrics</servlet-name>
    <servlet-class>br.com.labbs.monitor.exporter.MetricsServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>Metrics</servlet-name>
    <url-pattern>/metrics</url-pattern>
</servlet-mapping>
```

## Dependency state metrics

To add a dependency state metrics to the Monitor, you would implement the interface `DependencyChecker` and add an instance to the `MonitorMetrics` with the period interval that the dependency must be checked.

```java
DependencyChecker fakeChecker = new DependencyChecker() {
    @Override
    public DependencyState run() {
        // checking the database state
        return DependencyState.UP;
    }

    @Override
    public String getDependencyName() {
        return "fake-database-checker";
    }
};
long periodIntervalInMillis = 15000;
MonitorMetrics.INSTANCE.addDependencyChecker(fakeChecker, periodIntervalInMillis);
```

## Big Brother

This is part of a more large application called [Big Brother](https://github.com/labbsr0x/big-brother).

