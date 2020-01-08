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

1. The `request_seconds_bucket` metric defines the histogram of how many requests are falling into the well-defined buckets represented by the label `le`;

2. The `request_seconds_count` is a counter that counts the overall number of requests with those exact label occurrences;

3. The `request_seconds_sum` is a counter that counts the overall sum of how long the requests with those exact label occurrences are taking;

4. The `response_size_bytes` is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the `content-length` response header. If there is no such header, the value exposed as metric will be zero;

5. Finally, `dependency_up` is a metric to register whether a specific dependency is up (1) or down (0). The label `name` registers the dependency name;

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

```xml
<filter>
    <filter-name>metricsFilter</filter-name>
    <filter-class>br.com.labbs.monitor.filter.MetricsCollectorFilter</filter-class>
</filter>
<!-- This must be the first <filter-mapping> in the web.xml file so that you can get
the most accurate measurement of latency and response size. -->
<filter-mapping>
    <filter-name>metricsFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

> :warning: **NOTE**: 
> This must be the first `<filter-mapping>` in the `web.xml` file so that you can get the most accurate measurement of latency and response size.

#### Metrics Collector filter parameters

It is possible to use the following properties to configure the Metrics Collector Filter by init parameters on the web.xml file.


##### Override default buckets

The number of buckets is optionally overridable and can be configured by passing a comma-separated string of doubles as the `buckets` init parameter. 
The `buckets` default value is `0.1, 0.3, 1.5, 10.5`.

e.g.
```xml
<init-param>
    <param-name>buckets</param-name>
    <param-value>0.1,0.3,2,10</param-value>
</init-param>
```

##### Define max path depth

The max depth of the URI path(that is the value of `addr` label) can be configured by passing an integer value as the `path-depth` init parameter.
By default, the filter will provide the full path granularity. Any number provided that is less than one will provide the full path granularity.

e.g. 
```xml
<init-param>
    <param-name>path-depth</param-name>
    <param-value>1</param-value>
</init-param>
```

> :warning: **NOTE**: 
> Using full path granularity may affect performance

##### Exclude path from metrics collect

Exclusions of paths from collect can be configured by passing a comma-separated string of paths as the `exclusions` init parameter.

e.g. exclude paths starting with '/metrics' or '/static'
```xml
<init-param>
    <param-name>exclusions</param-name>
    <param-value>/metrics,/static</param-value>
</init-param>
```

### Exporting metrics

As well as the metrics filter, the class `MetricsServlet` can also be programmatically added to `javax.servlet.ServletContext` or initialized via `web.xml` file.

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

> :warning: **NOTE**: 
> The dependency checkers will run on a new thread, to prevent memory leak, make sure to call the method ``MonitorMetrics.INSTANCE.cancelAllDependencyCheckers()`` on undeploying/terminating the web app. 

## Big Brother

This project is part of a more large application called [Big Brother](https://github.com/labbsr0x/big-brother).

