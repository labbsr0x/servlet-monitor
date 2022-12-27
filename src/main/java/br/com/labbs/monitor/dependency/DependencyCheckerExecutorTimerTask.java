package br.com.labbs.monitor.dependency;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Executes scheduled dependency checkers with TimerTask.
 *
 * @see DependencyChecker
 */
public class DependencyCheckerExecutorTimerTask implements DependencyCheckerExecutor {
    private static final long START_DELAY_MILLIS = 10000L;

    private Timer timer;

    public DependencyCheckerExecutorTimerTask() {
        timer = new Timer("monitor-metrics-dependency-checker");
    }

    @Override
    public void cancelTasks() {
        timer.cancel();
        timer.purge();
    }

    @Override
    public void schedule(final TimerTask task, final long period) {
        timer.scheduleAtFixedRate(task, START_DELAY_MILLIS, period);
    }
}
