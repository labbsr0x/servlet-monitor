package br.com.labbs.monitor.filter;

import java.io.PrintWriter;
import java.util.Locale;

/**
 * A {@link PrintWriter} that counts the bytes written and provide methods to retrieve that amount.
 *
 * @author rogerio
 */
public class CountingPrintWriter extends PrintWriter {

    private final int newLine = count(System.lineSeparator());
    private final PrintWriter writer;
    private long count;

    /**
     * Creates an instance of {@link CountingPrintWriter}
     *
     * @param writer {@link PrintWriter}
     */
    public CountingPrintWriter(PrintWriter writer) {
        super(writer);
        this.writer = writer;
        DebugUtil.debug("CountingPrintWriter init");
    }

    /**
     * Returns the number of bytes written.
     *
     * @return amount of bytes written
     */
    public long getCount() {
        return count;
    }

    private void sum(CharSequence str) {
        if (str == null) {
            return;
        }
        int sum = 0;
        for (int i = 0; i < str.length(); i++) {
            char aChar = str.charAt(i);
            sum += CountingPrintWriter.this.count(aChar);
        }
        count += sum;
    }

    private void sum(char[] chars) {
        if (chars == null) {
            return;
        }

        int sum = 0;
        for (char aChar : chars) {
            sum += CountingPrintWriter.this.count(aChar);
        }
        count += sum;
    }

    private void sumNewLine() {
        count += newLine;
    }

    private void sumPrintLn(CharSequence s) {
        sum(s);
        sumNewLine();
    }

    private void sumPrintLn(char[] chars) {
        sum(chars);
        sumNewLine();
    }

    private int count(CharSequence str) {
        if (str == null) {
            return 0;
        }
        int sum = 0;
        for (int i = 0; i < str.length(); i++) {
            char aChar = str.charAt(i);
            sum += CountingPrintWriter.this.count(aChar);
        }
        return sum;
    }

    private int count(char aChar) {
        if ((aChar >= 0x0001) && (aChar <= 0x007F)) {
            return 1;
        } else {
            return (aChar > 0x07FF) ? 3 : 2;
        }
    }

    @Override
    public void write(String s) {
        this.writer.write(s);
        sum(s);
    }

    @Override
    public void write(char[] buf) {
        this.writer.write(buf);
        sum(buf);
    }

    @Override
    public void write(int c) {
        this.writer.write(c);
        count += CountingPrintWriter.this.count((char) c);
    }

    @Override
    public void write(String s, int off, int len) {
        this.writer.write(s, off, len);
        sum(s);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        this.writer.write(buf, off, len);
        sum(buf);
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        sum(csq);
        return this.writer.append(csq);
    }

    @Override
    public PrintWriter append(char c) {
        count += CountingPrintWriter.this.count(c);
        return this.writer.append(c);
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        sum(csq);
        return this.writer.append(csq, start, end);
    }

    @Override
    public boolean checkError() {
        return this.writer.checkError();
    }

    @Override
    public void close() {
        this.writer.close();
    }

    @Override
    public void flush() {
        this.writer.flush();
    }

    @Override
    public PrintWriter format(String format, Object... args) {
        return this.writer.format(format, args);
    }

    @Override
    public PrintWriter format(Locale l, String format, Object... args) {
        return this.writer.format(l, format, args);
    }

    @Override
    public void print(Object obj) {
        this.writer.print(obj);
        sum(String.valueOf(obj));
    }

    @Override
    public void print(String s) {
        this.writer.print(s);
        sum(s);
    }

    @Override
    public void print(boolean b) {
        this.writer.print(b);
        sum(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        this.writer.print(c);
        count += count(c);
    }

    @Override
    public void print(char[] s) {
        this.writer.print(s);
        sum(s);
    }

    @Override
    public void print(double d) {
        this.writer.print(d);
        sum(String.valueOf(d));
    }

    @Override
    public void print(float f) {
        this.writer.print(f);
        sum(String.valueOf(f));
    }

    @Override
    public void print(int i) {
        this.writer.print(i);
        sum(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        this.writer.print(l);
        sum(String.valueOf(l));
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        return this.writer.printf(format, args);
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object... args) {
        return this.writer.printf(l, format, args);
    }

    @Override
    public void println() {
        this.writer.println();
        sumNewLine();
    }

    @Override
    public void println(Object x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(String x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(boolean x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        this.writer.println(x);
        sumPrintLn(x);
    }

    @Override
    public void println(double x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        this.writer.println(x);
        sumPrintLn(String.valueOf(x));
    }

    @Override
    public String toString() {
        return this.writer.toString();
    }

}
