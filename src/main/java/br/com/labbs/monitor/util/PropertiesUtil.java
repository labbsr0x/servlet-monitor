package br.com.labbs.monitor.util;

import br.com.labbs.monitor.filter.DebugUtil;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    private PropertiesUtil() {
        // not intanciate this
    }
    
    public static String getValueFromPropertiesFile(String key) {
        return getValueFromPropertiesFile(key, "unknown") ;
    }
    
    public static String getValueFromPropertiesFile(String key, String defaulValue) {
        try {
            final Properties p = new Properties();
            final InputStream is = PropertiesUtil.class.getResourceAsStream("/application.properties");
            if (is != null) {
                p.load(is);
                //TODO check property existence
                return p.getProperty(key);
            }
            return defaulValue;
        } catch (Exception e) {
            DebugUtil.debug("error reading version from application.properties file: ", e.getMessage());
            return "error-reading-version";
        }
    }
}
