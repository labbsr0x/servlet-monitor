package br.com.labbs.monitor.dependency;

public enum DependencyState {

    DOWN(0), UP(1);

    private int value;

    DependencyState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
