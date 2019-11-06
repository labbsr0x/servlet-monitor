package br.com.labbs.monitor.exporter;

import br.com.labbs.monitor.MonitorMetrics;
import io.prometheus.client.exporter.common.TextFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * The MetricsServlet class exists to provide a simple way of exposing the metrics values.
 *
 */
public class MetricsServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
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

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

}
