package br.com.labbs.monitor.dependency;

/**
 * Defines the state a dependency can have.
 */
public enum DependencyState {

    DOWN(0), UP(1);

    private int value;

    /**
     * Creates a {@link DependencyState} by given value.
     *
     * @param value must be <code>1</code> to UP and <code>0</code> to DOWN
     */
    DependencyState(int value) {
        this.value = value;
    }

    /**
     * Returns the int code that represents the state.
     *
     * @return <code>1</code> to UP and <code>0</code> to DOWN
     */
    public int getValue() {
        return value;
    }

}
