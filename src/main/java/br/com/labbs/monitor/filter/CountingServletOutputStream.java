package br.com.labbs.monitor.filter;

import javax.servlet.ServletOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link ServletOutputStream} that counts the bytes written in the response and provide
 * methods to retrieve that amount.
 */
public class CountingServletOutputStream extends ServletOutputStream {

    private final CountingOutputStream output;

    public CountingServletOutputStream(ServletOutputStream output) {
        this.output = new CountingOutputStream(output);
        DebugUtil.debug("CountingServletOutputStream init");
    }

    /**
     * {@inheritDoc}
     * {@link ServletOutputStream#write(int)}
     */
    @Override
    public void write(int b) throws IOException {
        output.write(b);
    }

    /**
     * {@inheritDoc}
     * {@link ServletOutputStream#flush()}
     */
    @Override
    public void flush() throws IOException {
        output.flush();
    }

    /**
     * {@inheritDoc}
     * {@link ServletOutputStream#close()}
     */
    @Override
    public void close() throws IOException {
        output.close();
    }

    /**
     * Returns the number of bytes written to the {@link ServletOutputStream}
     *
     * @return number of bytes written to the response
     */
    public long getByteCount() {
        return output.getCount();
    }

    /**
     * Copyright (C) 2007 The Guava Authors
     * An OutputStream that counts the number of bytes written.
     *
     * @author Chris Nokleberg
     * @since 1.0
     */
    static final class CountingOutputStream extends FilterOutputStream {

        private long count;

        /**
         * Wraps another output stream, counting the number of bytes written.
         *
         * @param out the output stream to be wrapped
         */
        public CountingOutputStream(OutputStream out) {
            //check not null
            super(out);
        }

        /**
         * Returns the number of bytes written.
         */
        public long getCount() {
            return count;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        // Overriding close() because FilterOutputStream's close() method pre-JDK8 has bad behavior:
        // it silently ignores any exception thrown by flush(). Instead, just close the delegate stream.
        // It should flush itself if necessary.
        @Override
        public void close() throws IOException {
            out.close();
        }
    }

}