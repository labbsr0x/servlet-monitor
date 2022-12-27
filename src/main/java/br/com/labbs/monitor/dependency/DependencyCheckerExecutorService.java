package br.com.labbs.monitor.dependency;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Executes scheduled dependency checkers with ScheduledExecutorService.
 *
 * @see DependencyChecker
 */
public class DependencyCheckerExecutorService implements DependencyCheckerExecutor {
    private static final long START_DELAY_MILLIS = 10000L;

    private ScheduledExecutorService service;

    public DependencyCheckerExecutorService() {
        service = Executors.newScheduledThreadPool(5);
    }
    public DependencyCheckerExecutorService(String jndiName)  {
        try {
            ScheduledExecutorService executor
                    = (ScheduledExecutorService) new InitialContext().lookup(jndiName);
            
            service = executor;
        } catch (NamingException ex) {
            service = Executors.newScheduledThreadPool(5);
        }
    }

    @Override
    public void cancelTasks() {
        service.shutdown();
        try {
            service.awaitTermination(START_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            
        }
    }

    @Override
    public void schedule(final TimerTask task, final long period) {
        service.scheduleAtFixedRate(task, START_DELAY_MILLIS, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        return "DependencyCheckerExecutorService with: " + service;
    }
    
    
}
