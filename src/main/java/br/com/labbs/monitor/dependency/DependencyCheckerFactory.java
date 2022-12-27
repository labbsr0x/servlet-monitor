package br.com.labbs.monitor.dependency;

import br.com.labbs.monitor.util.PropertiesUtil;

public class DependencyCheckerFactory {

    private DependencyCheckerFactory() {
        // not intanciate this
    }

    /**
     * The factory uses properties file to create DependencyCheckerExecutor
     * impplementation.
     *
     * <p>
     * Params in application.properties:</p>
     *
     * <ul>
     * <li>application.executor.implementation=[TimerTask|ScheduledExecutorService]</li>
     * <li>application.executor.jndiname=[JNDI name to lookup
     * ScheduledExecutorService]</li>
     * </ul>
     *
     * @return the @{@link DependencyCheckerExecutor} implementation
     */
    public static DependencyCheckerExecutor create() {
        String executorImplementation = getExecutorImplementationFromPropertiesFile();

        DependencyCheckerExecutor executor;

        if ("TimerTask".equals(executorImplementation)) {
            executor = new DependencyCheckerExecutorTimerTask();
        } else if ("ScheduledExecutorService".equals(executorImplementation)) {
            String jndiName = getExecutorServiceJndiNameFromPropertiesFile();
            if ("unknown".equals(jndiName)) {
                executor = new DependencyCheckerExecutorService();
            } else {
                executor = new DependencyCheckerExecutorService(jndiName);
            }
        } else {
            executor = new DependencyCheckerExecutorTimerTask();
        }
        return executor;
    }

    private static String getExecutorImplementationFromPropertiesFile() {
        return PropertiesUtil.getValueFromPropertiesFile("application.executor.implementation");
    }

    private static String getExecutorServiceJndiNameFromPropertiesFile() {
        return PropertiesUtil.getValueFromPropertiesFile("application.executor.jndiname");
    }
}
