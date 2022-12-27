package br.com.labbs.monitor.dependency;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Executes scheduled dependency checkers.
 *
 * @see DependencyChecker
 */
public interface DependencyCheckerExecutor {

    /**
     * Terminates the executor timer, discarding any currently scheduled tasks.
     * Removes all cancelled tasks from the executor timer's task queue.
     */
    public void cancelTasks();

    /**
     * Schedules the specified task for repeated <i>fixed-rate execution period</i>.
     *
     * @param task   task to be executed
     * @param period time in milliseconds between successive task executions.
     */
    public void schedule(final TimerTask task, final long period);
}
