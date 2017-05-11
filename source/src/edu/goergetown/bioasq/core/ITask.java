package edu.goergetown.bioasq.core;

import edu.goergetown.bioasq.ui.ITaskListener;

/**
 * Created by Yektaie on 5/10/2017.
 */
public interface ITask {
    String getTitle();

    void process(ITaskListener listener);

    int getEstimatedTimeInMinutes();
}
