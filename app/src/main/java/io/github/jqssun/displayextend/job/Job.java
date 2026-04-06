package io.github.jqssun.displayextend.job;

public interface Job {
    void start() throws YieldException;
}
