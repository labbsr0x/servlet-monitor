package br.com.labbs.monitor;

import br.com.labbs.monitor.dependency.DependencyChecker;
import br.com.labbs.monitor.dependency.DependencyCheckerExecutor;
import br.com.labbs.monitor.dependency.DependencyState;
import io.prometheus.client.*;
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
                .labelNames("type", "status", "method", "addr" )
                .buckets(buckets)
                .register(collectorRegistry);

        responseSize = Counter.build().name(RESPONSE_SIZE_METRIC_NAME)
                .help("counts the size of each http response")
                .labelNames("type", "status", "method", "addr")
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
