package br.com.labbs.monitor.dependency;

import java.util.Timer;
import java.util.TimerTask;

public class DependencyCheckerExecutor {
    private static final long START_DELAY_MILLIS  = 10000L;

    private Timer timer;

    public DependencyCheckerExecutor() {
       timer = new Timer("monitor-metrics-dependency-checker");
    }
    public void cancelTasks(){
        timer.cancel();
        timer.purge();
    }

    /**
     * Schedules the specified task for repeated <i>fixed-rate execution period</i>.
     *
     * @param task task to be executed
     * @param period time in milliseconds between successive task executions.
     */
    public void schedule(final TimerTask task, final long period) {
        timer.scheduleAtFixedRate(task, START_DELAY_MILLIS, period);
    }
}
