package br.com.labbs.monitor.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

public class CountingServletResponse extends HttpServletResponseWrapper {

    private final CountingServletOutputStream output;
    private final PrintWriter writer;

    CountingServletResponse(HttpServletResponse response) throws IOException {
        super(response);
        output = new CountingServletOutputStream(response.getOutputStream());
        writer = new PrintWriter(output, true);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return output;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }

    long getByteCount() throws IOException {
        flushBuffer(); // Ensure that all bytes are written at this point.
        return output.getByteCount();
    }

    String getStatusRange() {
        int n = this.getStatus() / 100;
        return n + "xx";
    }

}