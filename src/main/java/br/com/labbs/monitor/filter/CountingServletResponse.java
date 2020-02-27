package br.com.labbs.monitor.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A {@link HttpServletResponse} that counts the bytes written in the response and provide
 * methods to retrieve that amount.
 */
public class CountingServletResponse extends HttpServletResponseWrapper {

    private final HttpServletResponse response;
    private CountingServletOutputStream output;
    private CountingPrintWriter writer;

    /**
     * Creates an instance of {@link CountingServletResponse} encapsulating the {@link HttpServletResponse}
     *
     * @param response response
     */
    CountingServletResponse(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    /**
     * {@inheritDoc}
     * {@link HttpServletResponseWrapper#getOutputStream()}
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (output == null) {
            output = new CountingServletOutputStream(response.getOutputStream());
        }
        return output;
    }

    /**
     * {@inheritDoc}
     * {@link HttpServletResponseWrapper#getWriter()}
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new CountingPrintWriter(response.getWriter());
        }
        return writer;
    }

    /**
     * {@inheritDoc}
     * {@link HttpServletResponseWrapper#flushBuffer()}
     */
    @Override
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    /**
     * Returns the number of bytes written to the response
     *
     * @return number of bytes written to the response
     */
    long getByteCount() {
        long count = 0;
        if (output != null) {
            count = output.getByteCount();
        } else if (writer != null) {
            count = writer.getCount();
        }
        return count;
    }

    /**
     * Returns the range of status code.
     * The first digit of the status code followed by XX suffix.
     *
     * @return status code range
     */
    String getStatusRange() {
        int n = this.getStatus() / 100;
        return n + "xx";
    }

}