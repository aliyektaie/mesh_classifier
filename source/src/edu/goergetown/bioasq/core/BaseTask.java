package edu.goergetown.bioasq.core;

import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;

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

    protected abstract ArrayList<SubTaskInfo> getSubTasks();

    @Override
    public int getEstimatedTimeInMinutes() {
        int result = 0;

        ArrayList<SubTaskInfo> tasks = getSubTasks();
        for (SubTaskInfo t : tasks) {
            result += t.estimation;
        }

        return result;
    }

    public int getEstimatedTimeBeforeTask(SubTaskInfo task) {
        int result = 0;

        ArrayList<SubTaskInfo> tasks = getSubTasks();
        for (SubTaskInfo t : tasks) {
            if (t == task)
                break;

            result += t.estimation;
        }

        return result;
    }

    protected void updateProgress(ITaskListener listener, SubTaskInfo task, int value, int max) {
        double total = getEstimatedTimeInMinutes() * 60;
        double before = getEstimatedTimeBeforeTask(task) * 60;
        double currentTaskShare = (task.estimation * 1.0 / getEstimatedTimeInMinutes());
        double currentPercent = (value * 1.0 / max);
        double current = currentPercent * currentTaskShare * task.estimation * 60;

        listener.setProgress((int)(before + current), (int)total);
    }
}
