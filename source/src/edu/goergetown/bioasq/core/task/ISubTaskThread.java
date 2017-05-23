package edu.goergetown.bioasq.core.task;

/**
 * Created by yektaie on 5/13/17.
 */
public interface ISubTaskThread {
    boolean isFinished();

    void start();

    double getProgress();
}
