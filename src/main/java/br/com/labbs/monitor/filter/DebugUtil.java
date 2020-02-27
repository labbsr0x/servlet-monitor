package br.com.labbs.monitor.filter;

/**
 * @author rogerio
 */
public class DebugUtil {

    private static boolean debug = false;

    public static void setDebug(boolean debug) {
        DebugUtil.debug = debug;
    }

    public static void setDebug(String debug) {
        setDebug(Boolean.parseBoolean(debug));
    }

    public static void debug(String msg) {
        if (debug) {
            System.out.println("DEBUG METRICS: " + msg);
        }
    }

    public static void debug(String msg1, long count) {
        if (debug) {
            debug(msg1 + count);
        }
    }

    public static void debug(String msg1, String msg2) {
        if (debug) {
            debug(msg1 + msg2);
        }
    }

    public static void debug(String msg1, String msg2, long count) {
        if (debug) {
            debug(msg1 + msg2 + count);
        }
    }

    public static void debug(String msg1, String msg2, String... msgs) {
        if (debug) {
            StringBuilder sb = new StringBuilder();
            sb.append(msg1).append(msg2);
            for (String string : msgs) {
                sb.append(string);
            }
            debug(sb.toString());
        }
    }

}
