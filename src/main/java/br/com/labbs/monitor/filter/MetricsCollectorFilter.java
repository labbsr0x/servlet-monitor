package br.com.labbs.monitor.filter;

import br.com.labbs.monitor.MonitorMetrics;
import io.prometheus.client.SimpleTimer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The MetricsFilter class provides a high-level filter that enables collection of (latency, amount and response
 * size metrics) for Servlet performance, based on schema, status code, HTTP method and URI path.
 *
 * <p>The Histogram buckets can be configured with a {@code buckets} init parameter whose value is a comma-separated list
 * of valid {@code double} values.
 *
 * Filter can be programmatically added to {@link ServletContext} or initialized via web.xml.
 *
 * Following code examples show possibles initializations:
 * Include filter in web.xml:
 * <pre>{@code
 * <filter>
 *   <filter-name>metricsFilter</filter-name>
 *   <filter-class>br.com.labbs.monitor.filter.MetricsFilter</filter-class>
 *   <init-param>
 *      <param-name>buckets</param-name>
 *      <param-value>0.005,0.05,0.1,0.5,1,2.5,5,7.5</param-value>
 *   </init-param>
 * </filter>
 * <filter-mapping>
 *   <filter-name>metricsFilter</filter-name>
 *   <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * }</pre>
 *
 * @author Werberson Silva &lt;werberson.silva@gmail.com&gt;
 */
public class MetricsCollectorFilter implements Filter {

    private static final String BUCKET_CONFIG_PARAM = "buckets";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        double[] buckets = null;
        if (filterConfig != null) {
            // Allow users to override the default bucket configuration
            if (!isEmpty(filterConfig.getInitParameter(BUCKET_CONFIG_PARAM))) {
                String[] bucketParams = filterConfig.getInitParameter(BUCKET_CONFIG_PARAM).split(",");
                buckets = new double[bucketParams.length];

                for (int i = 0; i < bucketParams.length; i++) {
                    buckets[i] = Double.parseDouble(bucketParams[i]);
                }
            }
        }
        MonitorMetrics.INSTANCE.init(true, buckets);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        final SimpleTimer timer = new SimpleTimer();
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final CountingServletResponse counterResponse = new CountingServletResponse((HttpServletResponse) response);

        try {
            chain.doFilter(httpRequest, counterResponse);
        } finally {
            final double elapsedSeconds = timer.elapsedSeconds();

            //TODO possible path parameters
            String path = httpRequest.getRequestURI();
            //TODO parameterize whether or not to add the context path
            //removes context path from URI
//            path = path.substring(httpRequest.getContextPath().length());
            final String method = httpRequest.getMethod();
            final String statusRange = Integer.toString(counterResponse.getStatus());
            //TODO why do not use a status code range instead of the specific one?
//            final String statusRange = counterResponse.getStatusRange();

            MonitorMetrics.INSTANCE.requestSeconds.labels(
                    httpRequest.getScheme(),
                    statusRange,
                    method,
                    path)
                    .observe(elapsedSeconds);
            MonitorMetrics.INSTANCE.responseSize.labels(httpRequest.getScheme(),
                    statusRange,
                    method,
                    path)
                    .inc(counterResponse.getByteCount());
        }
    }

    @Override
    public void destroy() {
        //ignored
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
