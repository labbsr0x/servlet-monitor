package br.com.labbs.monitor.dependency;

public interface DependencyChecker {

    DependencyState run();

    String getDependencyName();

}
