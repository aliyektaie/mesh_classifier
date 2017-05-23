package edu.goergetown.bioasq.core.task;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class SubTaskInfo {
    public int estimation = 0;
    public String title = "";

    public SubTaskInfo(String title, int estimatedTime) {
        this.title = title;
        this.estimation = estimatedTime;
    }
}
