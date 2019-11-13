package br.com.labbs.monitor.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

public class CountingServletResponse extends HttpServletResponseWrapper {

    private CountingServletOutputStream output;
    private CountingPrintWriter writer;
    private final HttpServletResponse response;

    CountingServletResponse(HttpServletResponse response) throws IOException {
        super(response);
        this.response = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (output == null) {
            output = new CountingServletOutputStream(response.getOutputStream());
        }
        return output;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new CountingPrintWriter(response.getWriter());
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    long getByteCount() throws IOException {
        long count = 0;
        if (output != null) {
            count = output.getByteCount();
        } else if (writer != null) {
            count = writer.getCount();
        }
        return count;
    }

    String getStatusRange() {
        int n = this.getStatus() / 100;
        return n + "xx";
    }

}