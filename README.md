# servlet-monitor

A Servlet filter to add basic but very useful [Prometheus](https://prometheus.io) metrics for your app.

## Metrics

The only exposed metrics are the following:

```
request_seconds_bucket{type, status, isError, method, addr, le}
request_seconds_count{type, status, isError, method, addr}
request_seconds_sum{type, status, isError, method, addr}
response_size_bytes{type, status, isError, method, addr}
dependency_up{name}
dependency_request_seconds_bucket{name, type, status, isError, errorMessage, method, addr, le}
dependency_request_seconds_count{name, type, status, isError, errorMessage, method, add}
dependency_request_seconds_sum{name, type, status, isError, errorMessage, method, add}
application_info{version}
```

Details:

1. The `request_seconds_bucket` metric defines the histogram of how many requests are falling into the well-defined buckets represented by the label `le`;

2. The `request_seconds_count` is a counter that counts the overall number of requests with those exact label occurrences;

3. The `request_seconds_sum` is a counter that counts the overall sum of how long the requests with those exact label occurrences are taking;

4. The `response_size_bytes` is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the `content-length` response header. If there is no such header, the value exposed as metric will be zero;

5. The `dependency_up` is a metric to register whether a specific dependency is up (1) or down (0). The label `name` registers the dependency name;

6. The `dependency_request_seconds_bucket` is a metric that defines the histogram of how many requests to a specific dependency are falling into the well defined buckets represented by the label le;

7. The `dependency_request_seconds_count` is a counter that counts the overall number of requests to a specific dependency;

8. The `dependency_request_seconds_sum` is a counter that counts the overall sum of how long requests to a specific dependency are taking;

9. The `application_info` holds static info of an application, such as it's semantic version number;

Labels:

1. `type` tells which request protocol was used (e.g. `grpc` or `http`);

2. `status` registers the response status (e.g. HTTP status code);

3. `method` registers the request method;

4. `addr` registers the requested endpoint address;

5. `version` tells which version of your app handled the request;

6. `isError` lets us know if the status code reported is an error or not;

7. `errorMessage` registers the error message;

8. `name` registers the name of the dependency;

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
**Please use the latest version:**  [![Released Version](https://img.shields.io/maven-central/v/br.com.labbs/servlet-monitor.svg?maxAge=2000)](https://search.maven.org/search?q=br.com.labbs)

### Collecting metrics

The collector filter can be programmatically added to `javax.servlet.ServletContext` or initialized by `web.xml` file.

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

##### JVM metrics export

It is possible to enable/disable the JVM metrics export.
The export is enabled by default.

e.g. disabling
```xml
<init-param>
    <param-name>export-jvm-metrics</param-name>
    <param-value>false</param-value>
</init-param>
```

#### Setting application version

##### Manually
To provide the application version to the metrics collector, the `application.properties` file must exist in the project at the project resources path(Maven projects default path `src/main/resources`) with the application version set to `application.version` property.

e.g. `src/main/resources/application.properties`
```properties
application.version=1.0.2
```

Make sure the file `classes/application.properties` exist into your jar or war package.

##### From Maven pom.xml file

The process to automatically set application version retrieving the project version from the pom.xml file is using [Maven resource filtering](https://maven.apache.org/guides/getting-started/index.html#How_do_I_filter_resource_files).
To have Maven filter resources when copying, simply set `filtering` to true for the resource directory in your `pom.xml`.
```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>
        </resource>
    </resources>
</build>
```

Create the file `application.propeties` at the application resources path(most commonly `src/main/resources`) and add the `application.version=${project.version}` property whose value `${project.version}` will be supplied when the resource is filtered. 

e.g. `src/main/resources/application.properties`
```properties
application.version=${project.version}
```

Make sure the file `classes/application.properties` exist into your jar or war package and the property value is the same of the pom.xml file.

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

You also can monitore a dependency event. Just call `addDependencyEvent` and pass the right params.

```java
MonitorMetrics.INSTANCE.addDependencyEvent(name, type, status, method, address, isError, errorMessage, elapsedSeconds);
```

## Big Brother

This project is part of a more large application called [Big Brother](https://github.com/labbsr0x/big-brother).

