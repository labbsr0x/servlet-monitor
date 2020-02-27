package br.com.labbs.monitor.dependency;

/**
 * Defines methods that all dependency checkers must implement.
 */
public interface DependencyChecker {

    /**
     * Actually perform the check and return the state of the dependency.
     *
     * @return UP or DOWN, the state of the dependency.
     */
    DependencyState run();

    /**
     * Returns the name of the dependency
     *
     * @return The name of the dependency
     */
    String getDependencyName();

}
