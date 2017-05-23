package edu.goergetown.bioasq.ui;

/**
 * Created by yektaie on 4/18/17.
 */
public interface ITaskListener {
    void setCurrentState(String state);

    void setProgress(int value, int max);

    void log(String entry);

    void saveLogs(String path);
}
