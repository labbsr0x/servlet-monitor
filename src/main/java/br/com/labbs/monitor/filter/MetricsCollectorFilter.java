package br.com.labbs.monitor.filter;

import br.com.labbs.monitor.MonitorMetrics;
import io.prometheus.client.SimpleTimer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The MetricsFilter class provides a high-level filter that enables collection of (latency, amount and response
 * size metrics) for Servlet performance, based on schema, status code, HTTP method and URI path.
 *
 * <p>The Histogram buckets can be configured with a {@code buckets} init parameter whose value is a comma-separated list
 * of valid {@code double} values.
 * <p>
 * Filter can be programmatically added to {@link ServletContext} or initialized via web.xml.
 * <p>
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
    private static final String PATH_DEPTH_PARAM = "path-depth";
    private static final String EXCLUSIONS = "exclusions";
    private static final String VERSION = "version";
    private static final String DEBUG = "debug";
    private final List<String> exclusions = new ArrayList<String>();

    private int pathDepth = 0;
    private String version;

    @Override
    public void init(FilterConfig filterConfig) {

        this.version = getApplicationVersionFromPropertiesFile();

        double[] buckets = null;
        if (filterConfig != null) {
            String debugParam = filterConfig.getInitParameter(DEBUG);
            if (isNotEmpty(debugParam)) {
                DebugUtil.setDebug(debugParam);
            }
            String versionParam = filterConfig.getInitParameter(VERSION);
            if (isNotEmpty(versionParam)) {
                this.version = versionParam;
            }
            // Allow overriding of the path "depth" to track
            String pathDepthStr = filterConfig.getInitParameter(PATH_DEPTH_PARAM);
            if (isNotEmpty(pathDepthStr)) {
                try {
                    pathDepth = Integer.parseInt(pathDepthStr);
                } catch (NumberFormatException e) {
                    DebugUtil.debug("Error: " + PATH_DEPTH_PARAM + " must be an int value but got '" + pathDepthStr + "'.");
                }
            }
            // Allow users to override the default bucket configuration
            String bucketsParam = filterConfig.getInitParameter(BUCKET_CONFIG_PARAM);
            if (isNotEmpty(bucketsParam)) {
                String[] bucketParams = bucketsParam.split(",");
                buckets = new double[bucketParams.length];

                for (int i = 0; i < bucketParams.length; i++) {
                    buckets[i] = Double.parseDouble(bucketParams[i]);
                }
            }
            // Allow users to define paths to be excluded from metrics collect
            String exclusionsParam = filterConfig.getInitParameter(EXCLUSIONS);
            if (isNotEmpty(exclusionsParam)) {
                String[] arrayExclusions = exclusionsParam.split(",");
                for (String string : arrayExclusions) {
                    exclusions.add(string.trim());
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

        // TODO parameterize whether or not to add the context path
        String path = httpRequest.getRequestURI();
        path = substringMaxDepth(path, pathDepth);

        if (isExcludedPath(httpRequest, path)) {
            chain.doFilter(request, response);
        } else {
            final CountingServletResponse counterResponse =
                    new CountingServletResponse((HttpServletResponse) response);
            try {
                chain.doFilter(httpRequest, counterResponse);
            } finally {
                collect(httpRequest, counterResponse, path, timer.elapsedSeconds());
            }
        }
    }

    @Override
    public void destroy() {
        //ignored
    }

    private boolean isExcludedPath(final HttpServletRequest httpRequest, String path) {
        if (path.startsWith(httpRequest.getContextPath())) {
            path = path.substring(httpRequest.getContextPath().length());
        }
        for (String exclusion : exclusions) {
            if (path.startsWith(exclusion)) {
                DebugUtil.debug("Excluded ", path);
                return true;
            }
        }
        return false;
    }

    private void collect(HttpServletRequest httpRequest, CountingServletResponse counterResponse, String path, double elapsedSeconds) throws IOException {
        final String method = httpRequest.getMethod();
        final String status = Integer.toString(counterResponse.getStatus());
        final boolean isError = isErrorStatus(counterResponse.getStatus());
        final long count = counterResponse.getByteCount();
        final String scheme = httpRequest.getScheme();
        DebugUtil.debug(path, " ; bytes count = ", count);
        MonitorMetrics.INSTANCE.collectTime(scheme, status, method, path, this.version, isError, elapsedSeconds);
        MonitorMetrics.INSTANCE.collectSize(scheme, status, method, path, this.version, isError, count);
    }

    private boolean isErrorStatus(int status) {
        return status < 200 || status >= 400;
    }

    private boolean isNotEmpty(String s) {
        return s != null && s.trim().length() != 0;
    }

    private String substringMaxDepth(String path, int pathDepth) {
        if (path == null || pathDepth < 1) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        int count = 0;
        int i = -1;
        do {
            i = path.indexOf("/", i + 1);
            if (i < 0) {
                // Path depth is shorter than specified pathDepth.
                return path;
            }
            count++;
        } while (count <= pathDepth);
        return path.substring(0, i);
    }

    private String getApplicationVersionFromPropertiesFile() {
        try {
            final Properties p = new Properties();
            final InputStream is = getClass().getResourceAsStream("/application.properties");
            if (is != null) {
                p.load(is);
                return p.getProperty("application.version");
            }
            return "unknown";
        } catch (Exception e) {
            DebugUtil.debug("error reading version from application.properties file: ", e.getMessage());
            return "error-reading-version";
        }
    }

}
