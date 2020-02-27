package br.com.labbs.monitor.exporter;

import br.com.labbs.monitor.MonitorMetrics;
import io.prometheus.client.Gauge;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MetricsServletTest {

    private MetricsServlet metricsServlet = new MetricsServlet();

    @AfterClass
    public static void clearRegistry() {
        MonitorMetrics.INSTANCE.collectorRegistry.clear();
    }

    @Before
    public void clear() {
        clearRegistry();
    }

    @Test
    public void test_writer_all_registered_collectors() throws IOException {
        Gauge.build("a", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);
        Gauge.build("b", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);
        Gauge.build("c", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);

        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);

        Mockito.when(resp.getWriter()).thenReturn(writer);

        metricsServlet.doGet(req, resp);

        final String respBody = stringWriter.toString();

        Assert.assertThat(respBody, CoreMatchers.containsString("a 0.0"));
        Assert.assertThat(respBody, CoreMatchers.containsString("b 0.0"));
        Assert.assertThat(respBody, CoreMatchers.containsString("c 0.0"));
    }

    @Test
    public void test_writer_is_closed_normally() throws IOException {
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        final PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(resp.getWriter()).thenReturn(writer);
        Gauge.build("a", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);

        metricsServlet.doGet(req, resp);
        Mockito.verify(writer).close();
    }

    @Test
    public void test_writer_is_closed_on_exception() throws IOException {
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        final PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(resp.getWriter()).thenReturn(writer);
        Mockito.doThrow(new RuntimeException()).when(writer).write(Mockito.anyChar());
        Mockito.doThrow(new RuntimeException()).when(writer).write(Mockito.anyInt());
        Gauge.build("a", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);

        try {
            metricsServlet.doGet(req, resp);
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Mockito.verify(writer).close();
        }
    }

    @Test
    public void test_post_and_get_methods_must_return_the_same_content() throws IOException {
        Gauge.build("a", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);
        Gauge.build("b", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);
        Gauge.build("c", "a help").register(MonitorMetrics.INSTANCE.collectorRegistry);

        final HttpServletRequest reqGet = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse respGet = Mockito.mock(HttpServletResponse.class);
        final StringWriter stringWriterGet = new StringWriter();
        final PrintWriter writerGet = new PrintWriter(stringWriterGet);
        Mockito.when(respGet.getWriter()).thenReturn(writerGet);
        metricsServlet.doGet(reqGet, respGet);

        final String contentGet = stringWriterGet.toString();

        final HttpServletRequest reqPost = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse respPost = Mockito.mock(HttpServletResponse.class);
        final StringWriter stringWriterPost = new StringWriter();
        final PrintWriter writerPost = new PrintWriter(stringWriterPost);
        Mockito.when(respPost.getWriter()).thenReturn(writerPost);
        metricsServlet.doPost(reqPost, respPost);

        final String contentPost = stringWriterPost.toString();

        Assert.assertEquals(contentGet, contentPost);
    }
}