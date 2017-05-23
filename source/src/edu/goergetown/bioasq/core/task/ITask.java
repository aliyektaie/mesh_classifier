package edu.goergetown.bioasq.core.task;

import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/10/2017.
 */
public interface ITask {
    String getTitle();

    void process(ITaskListener listener);

    int getEstimatedTimeInMinutes();

    Hashtable<String, ArrayList<Object>> getParameters();

    void setParameter(String name, Object value);

    Object getParameter(String name);
}
