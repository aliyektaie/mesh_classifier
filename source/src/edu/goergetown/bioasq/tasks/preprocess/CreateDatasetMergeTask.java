package edu.goergetown.bioasq.tasks.preprocess;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by Yektaie on 5/11/2017.
 */
public class CreateDatasetMergeTask extends BaseTask {
    private SubTaskInfo MERGING_TASK = new SubTaskInfo("Merging dataset for parallellization", 10);
    private int numberOfClusters = 32;

    @Override
    public void process(ITaskListener listener) {
        mergeEntries(listener);
    }

    private void mergeEntries(ITaskListener listener) {
        listener.log("Start merging sub files");
        ArrayList<String> directories = FileUtils.getDirectories(Constants.DATA_FOLDER_BY_YEAR);
        int i = 0;
        for (String folder : directories) {
            updateProgress(listener, MERGING_TASK, i, directories.size());
            mergeEntries(folder);

            i++;
        }
    }

    private void mergeEntries(String folder) {
        String year = folder.substring(folder.lastIndexOf("\\") + 1);
        ArrayList<String> files = FileUtils.getFiles(folder);

        File file = new File(Constants.DATA_FOLDER_BY_YEAR + year + ".txt");

        try {
            FileOutputStream fs = new FileOutputStream(file);

            for (String path: files) {
                fs.write(FileUtils.readTextFile(path).getBytes("utf8"));
                FileUtils.delete(path);
            }

            fs.flush();
            fs.close();
        } catch (Exception e) {

        }

        FileUtils.delete(folder);
    }


    @Override
    protected String getTaskClass() {
        return CLASS_PREPROCESS;
    }

    @Override
    protected String getTaskTitle() {
        return "Create Dataset Cluster";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(MERGING_TASK);

        return result;
    }
}
