package edu.goergetown.bioasq.core;

import edu.goergetown.bioasq.ui.ITaskListener;

/**
 * Created by Yektaie on 5/10/2017.
 */
public abstract class BaseTask implements ITask {
    protected final String CLASS_PREPROCESS = "Pre-process";
    protected final String CLASS_TRAIN = "Train";
    protected final String CLASS_CLASSIFIER = "Classifier";

    public String getTitle() {
        return getTaskClass() + " - " + getTaskTitle();
    }

    public abstract void process(ITaskListener listener);

    protected abstract String getTaskClass();

    protected abstract String getTaskTitle();

    @Override
    public String toString() {
        return getTitle();
    }
}
