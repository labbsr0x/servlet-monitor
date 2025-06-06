package br.com.labbs.monitor.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import br.com.labbs.monitor.MonitorMetrics;
import io.prometheus.client.SimpleTimer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
 *   <filter-class>br.com.labbs.monitor.filter.MetricsCollectorFilter</filter-class>
 *   <init-param>
 *      <param-name>buckets</param-name>
 *      <param-value>>0.1,0.3,2,10</param-value>
 *   </init-param>
 * </filter>
 * <filter-mapping>
 *   <filter-name>metricsFilter</filter-name>
 *   <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * }</pre>
 * <p>
 * See the <a href="https://github.com/labbsr0x/servlet-monitor">documentation</a> to learn how to use this filter.
 *
 * @author Werberson Silva &lt;werberson.silva@gmail.com&gt;
 */
public class MetricsCollectorFilter implements Filter {

    private static final String EXPORT_JVM_METRICS_PARAM = "export-jvm-metrics";
    private static final String BUCKET_CONFIG_PARAM = "buckets";
    private static final String PATH_DEPTH_PARAM = "path-depth";
    private static final String EXCLUSIONS = "exclusions";
    private static final String ERROR_MESSAGE_PARAM = "error-message";
    private static final String DEBUG = "debug";
    private static final String APPLICATION_VERSION = "application-version";
    private static final String DEFAULT_FILTER_REGEX = "[^A-zÀ-ú .,]+";
    private static final String FILTER_REGEX_PARAM = "error-info-regex";
    private static final String ENABLE_ERROR_REGEX_DEPENDENCY = "enable-error-regex-dependency";
    private static final String FILTER_MAX_SIZE_PARAM = "error-info-max-size";
    private static final Logger LOGGER = Logger.getLogger(MetricsCollectorFilter.class.getName());
    private final List<String> exclusions = new ArrayList<String>();
    private int filter_max_size = 50;
    private String filter_regex = "";
    private boolean enableErrorDependencyRegex = true; //defaut


    private int pathDepth = 0;
    private String errorMessageParam = "";

    /**
     * {@inheritDoc}
     * {@link Filter#init(FilterConfig)}
     */
    @Override
    public void init(FilterConfig filterConfig) {
        double[] buckets = null;
        boolean exportJvmMetrics = true;
        String exportApplicationVersion = "";
        if (filterConfig != null) {
            String debugParam = filterConfig.getInitParameter(DEBUG);
            if (isNotEmpty(debugParam)) {
                DebugUtil.setDebug(debugParam);
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
            // Allow users to enable/disable the JVM metrics export
            String exportJvmMetricsStr = filterConfig.getInitParameter(EXPORT_JVM_METRICS_PARAM);
            if (isNotEmpty(exportJvmMetricsStr)) {
                exportJvmMetrics = Boolean.parseBoolean(exportJvmMetricsStr);
            }
            
            String enableErrorRegexDependencyParam = filterConfig.getInitParameter(ENABLE_ERROR_REGEX_DEPENDENCY);
            if ("false".equalsIgnoreCase(enableErrorRegexDependencyParam)) {
                enableErrorDependencyRegex = false;
            }
            
            exportApplicationVersion = filterConfig.getInitParameter(APPLICATION_VERSION);

            filter_max_size = filterConfig.getInitParameter(FILTER_MAX_SIZE_PARAM) != null ?
                Integer.valueOf(filterConfig.getInitParameter(FILTER_MAX_SIZE_PARAM)) : filter_max_size;
            filter_regex = filterConfig.getInitParameter(FILTER_REGEX_PARAM) != null ?
                filterConfig.getInitParameter(FILTER_REGEX_PARAM) : DEFAULT_FILTER_REGEX;
        }
        String version = isNotEmpty(exportApplicationVersion) ? exportApplicationVersion : getApplicationVersionFromPropertiesFile();
        // Allow users to capture error messages
        errorMessageParam = filterConfig.getInitParameter(ERROR_MESSAGE_PARAM);
        
        String dependencyErrorRegex = null;
        if (enableErrorDependencyRegex) {
        	dependencyErrorRegex = filter_regex;
        }
        
        MonitorMetrics.INSTANCE.init(exportJvmMetrics, version, dependencyErrorRegex, buckets);
    }

    /**
     * {@inheritDoc}
     * {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     */
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

    /**
     * {@inheritDoc}
     * {@link Filter#destroy()}
     */
    @Override
    public void destroy() {
        //ignored
    }

    /**
     * Checks whether the path is configured to be ignored from the metrics collection.
     *
     * @param httpRequest request
     * @param path        HTTP request path
     * @return <code>true</code> if the path is configured to be excluded.
     */
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

    /**
     * Collect metrics
     *
     * @param httpRequest     request
     * @param counterResponse response
     * @param path            path
     * @param elapsedSeconds  how long time did the request has executed
     */
    private void collect(HttpServletRequest httpRequest, CountingServletResponse counterResponse, String path, double elapsedSeconds) {
    	final String method = httpRequest.getMethod();
        final String status = Integer.toString(counterResponse.getStatus());
        final boolean isError = isErrorStatus(counterResponse.getStatus());
        final String errorMessage = getErrorMessage(httpRequest);
        final long count = counterResponse.getByteCount();
        final String scheme = httpRequest.getScheme();
        DebugUtil.debug(path, " ; bytes count = ", count);
        MonitorMetrics.INSTANCE.collectTime(scheme, status, method, path, isError, errorMessage, elapsedSeconds);
        MonitorMetrics.INSTANCE.collectSize(scheme, status, method, path, isError, errorMessage, count);
    }

    /**
     * Checks if the parameters is a HTTP status code error
     *
     * @param status HTTP status code
     * @return <code>true</code> if the status code is lower than 200 or greater than or equals 400
     */
    private boolean isErrorStatus(int status) {
        return status < 200 || status >= 400;
    }

    /**
     * Get the error message from a request.
     * If error message is null, sets the string to empty string.
     * If a regex is defined, use it to filter message
     * 
     * Default regex: [^A-zÀ-ú .,]+
     * Default max size: 50
     *
     * @param httpRequest request
     * @return string with the error message or empty string if error message not found.
     */
    private String getErrorMessage(HttpServletRequest httpRequest) {

        if (errorMessageParam == null) {
            return "";
        }
        String errorMessage = (String) httpRequest.getAttribute(errorMessageParam);
        String result = "";
        if (errorMessage == null) {
            return result;
        }

        try {
            // apply whitelist filter
            result = errorMessage.replaceAll(filter_regex, "");
            if (result.length() > filter_max_size) {
                result = result.substring(0, filter_max_size);
            }
        } catch (Exception e) {
            // avoid invalid regex or invalid matcher group index
            result = "";
            LOGGER.warning("Invalid regex: " + e.getMessage());
        }
        return result;
    }

    /**
     * Checks if a {@link String} is empty
     *
     * @param s {@link String} to be checked
     * @return <code>true</code> if the String is empty
     */
    private boolean isNotEmpty(String s) {
        return s != null && s.trim().length() != 0;
    }

    /**
     * Returns a substring of the <code>path</code> based on depth count of slash char "/".
     *
     * @param path      HTTP request path
     * @param pathDepth how many slash "/" to be include on substring. anything less than 1 means full granularity.
     * @return substring
     */
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

    /**
     * Reads the file "application.properties" located in resource folder and return the "application.version"
     * property value.
     * If the file not exists, {@link String} "unknown" will be returned.
     * If an error occurs while reading the file, {@link String} "error-reading-version" will be returned.
     *
     * @return value read from the file or "unknown" when file not exist or "error-reading-version" when error occurs.
     */
    private String getApplicationVersionFromPropertiesFile() {
        try {
            final Properties p = new Properties();
            final InputStream is = getClass().getResourceAsStream("/application.properties");
            if (is != null) {
                p.load(is);
                //TODO check property existence
                return p.getProperty("application.version");
            }
            return "unknown";
        } catch (Exception e) {
            DebugUtil.debug("error reading version from application.properties file: ", e.getMessage());
            return "error-reading-version";
        }
    }

}
