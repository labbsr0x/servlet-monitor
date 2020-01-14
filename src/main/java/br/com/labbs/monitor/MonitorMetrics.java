package br.com.labbs.monitor;

import br.com.labbs.monitor.dependency.DependencyChecker;
import br.com.labbs.monitor.dependency.DependencyCheckerExecutor;
import br.com.labbs.monitor.dependency.DependencyState;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;

import java.util.TimerTask;

/**
 * Singleton MonitorMetrics provides the three following Prometheus metrics:
 *
 * <pre>
 * {@code
 * Histogram requestSeconds:
 *    request_seconds_bucket{type,status, method, addr, le}
 *    request_seconds_count{type, status, method, addr}
 *    request_seconds_sum{type, status, method, addr}
 *
 * Counter responseSize:
 *    response_size_bytes{type, status, method, addr}
 *
 * Gauge dependencyUp:
 *    dependency_up{name}
 * }
 * </pre>
 *
 * @author Werberson Silva &lt;werberson.silva@gmail.com&gt;
 */
public enum MonitorMetrics {

    INSTANCE;

    public CollectorRegistry collectorRegistry = new CollectorRegistry(true);

    private static final String REQUESTS_SECONDS_METRIC_NAME = "request_seconds";
    private static final String RESPONSE_SIZE_METRIC_NAME = "response_size_bytes";
    private static final String DEPENDENCY_UP_METRIC_NAME = "dependency_up";
    private static double[] DEFAULT_BUCKETS = {0.1D, 0.3D, 1.5D, 10.5D};

    public Histogram requestSeconds;
    public Counter responseSize;
    public Gauge dependencyUp;

    private DependencyCheckerExecutor dependencyCheckerExecutor = new DependencyCheckerExecutor();

    private boolean initialized;

    /**
     * Initialize metric collectors
     *
     * @param collectJvmMetrics collect or not JVM metrics
     * @param buckets the numbers of buckets
     */
    public void init(boolean collectJvmMetrics, double... buckets) {
        if (initialized) {
            throw new IllegalStateException("The MonitorMetrics instance has already been initialized. " +
                    "The MonitorMetrics.INSTANCE.init method must be executed only once");
        }
        if (buckets == null || buckets.length == 0) {
            buckets = DEFAULT_BUCKETS;
        }

        requestSeconds = Histogram.build().name(REQUESTS_SECONDS_METRIC_NAME)
                .help("records in a histogram the number of http requests and their duration in seconds")
                .labelNames("type", "status", "method", "addr", "version", "isError")
                .buckets(buckets)
                .register(collectorRegistry);

        responseSize = Counter.build().name(RESPONSE_SIZE_METRIC_NAME)
                .help("counts the size of each http response")
                .labelNames("type", "status", "method", "addr", "version", "isError")
                .register(collectorRegistry);

        dependencyUp = Gauge.build().name(DEPENDENCY_UP_METRIC_NAME)
                .help("records if a dependency is up or down. 1 for up, 0 for down")
                .labelNames("name")
                .register(collectorRegistry);

        if (collectJvmMetrics) {
            DefaultExports.register(collectorRegistry);
        }

        initialized = true;
    }

    /**
     *
     * Collect latency metric request_seconds
     *
     * @param type which request protocol was used (e.g. grpc or http)
     * @param status the response status(e.g. response HTTP status code)
     * @param method the request method(e.g. HTTP methods GET, POST, PUT)
     * @param addr the requested endpoint address
     * @param version which version of your app handled the request
     * @param isError if the status code reported is an error or not
     * @param elapsedSeconds how long time did the request has executed
     */
    public void collectTime(String type, String status, String method, String addr, String version, boolean isError, double elapsedSeconds) {
        if (initialized) {
            requestSeconds.labels(type, status, method, addr, version, Boolean.toString(isError))
                    .observe(elapsedSeconds);
        }
    }

    /**
     *
     * Collect size metric response_size_bytes
     *
     * @param type which request protocol was used (e.g. grpc or http)
     * @param status the response status(e.g. response HTTP status code)
     * @param method the request method(e.g. HTTP methods GET, POST, PUT)
     * @param addr the requested endpoint address
     * @param version which version of your app handled the request
     * @param isError if the status code reported is an error or not
     * @param size the response content size
     */
    public void collectSize(String type, String status, String method, String addr, String version, boolean isError, final long size) {
        if (initialized) {
            MonitorMetrics.INSTANCE.responseSize.labels(type, status, method, addr, version, Boolean.toString(isError)).inc(size);
        }
    }

    /**
     * Cancel all scheduled dependency checkers and terminates the executor timer.
     */
    public void cancelAllDependencyCheckers(){
        dependencyCheckerExecutor.cancelTasks();
    }

    /**
     * Add dependency to be checked successive between the period
     *
     * @param checker dependency checker
     * @param period time in milliseconds between successive task executions.
     */
    public void addDependencyChecker(final DependencyChecker checker, final long period) {
        TimerTask task = new TimerTask() {
            public void run() {
                if (!initialized) {
                    //skipping, the MonitorMetrics instance has not been initialized yet.
                    //MonitorMetrics.INSTANCE.init must be executed once.
                    return;
                }
                DependencyState state = checker.run();
                dependencyUp.labels(checker.getDependencyName()).set(state.getValue());
            }
        };
        dependencyCheckerExecutor.schedule(task, period);
    }
 
}
