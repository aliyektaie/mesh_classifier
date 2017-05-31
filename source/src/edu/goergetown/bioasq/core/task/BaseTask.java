package edu.goergetown.bioasq.core.task;

import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/10/2017.
 */
public abstract class BaseTask implements ITask {
    protected final String CLASS_PREPROCESS = "Pre-process";
    protected final String CLASS_TRAIN = "Train";
    protected final String CLASS_CLASSIFIER = "Evaluation";

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

    public void updateProgress(ITaskListener listener, SubTaskInfo task, long value, long max) {
        double total = getEstimatedTimeInMinutes() * 60;
        double before = getEstimatedTimeBeforeTask(task) * 60;
        double currentTaskShare = (task.estimation * 1.0 / getEstimatedTimeInMinutes());
        double currentPercent = (value * 1.0 / max);
        double current = currentPercent * currentTaskShare * total;

        listener.setProgress((int)(before + current), (int)total);
        listener.setCurrentState(task.title);
    }

    public void executeWorkerThreads(ITaskListener listener, SubTaskInfo task, ArrayList<ISubTaskThread> threads) {
        for (ISubTaskThread t : threads) {
            t.start();
        }

        while (!allThreadsFinished(threads)) {
            double progress = getProgress(threads);
            updateProgress(listener, task, (int) (progress * 100), 10000);

            waitFor(200);
        }
    }

    protected double getProgress(ArrayList<ISubTaskThread> threads) {
        double result = 0;

        for (ISubTaskThread t : threads) {
            result+=t.getProgress();
        }

        return result / threads.size();
    }

    protected void waitFor(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {

        }
    }

    protected boolean allThreadsFinished(ArrayList<ISubTaskThread> threads) {
        boolean result = true;

        for (ISubTaskThread t : threads) {
            result = result && t.isFinished();
        }

        return result;
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        return null;
    }

    @Override
    public void setParameter(String name, Object value) {

    }

    @Override
    public Object getParameter(String name) {
        return null;
    }
}
