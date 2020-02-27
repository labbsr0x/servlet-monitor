package br.com.labbs.monitor.exporter;

import br.com.labbs.monitor.MonitorMetrics;
import io.prometheus.client.exporter.common.TextFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Provides a simple way of exposing the metrics values.
 */
public class MetricsServlet extends HttpServlet {

    /**
     * {@inheritDoc}
     * {@link javax.servlet.http.HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(TextFormat.CONTENT_TYPE_004);

        Writer writer = resp.getWriter();
        try {
            TextFormat.write004(writer, MonitorMetrics.INSTANCE.collectorRegistry.metricFamilySamples());
            writer.flush();
        } finally {
            writer.close();
        }
    }

    /**
     * {@inheritDoc}
     * {@link javax.servlet.http.HttpServlet#doPost(HttpServletRequest, HttpServletResponse)}
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

}
