package edu.goergetown.bioasq.tasks.experiments;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

/**
 * Created by Yektaie on 6/1/2017.
 */
public class CalculateOveralScoreTask extends BaseTask {
    @Override
    public void process(ITaskListener listener) {
        String path = Constants.TEMP_FOLDER + "text-classification-mesh-term-bm25f.csv";
        String[] lines = FileUtils.readAllLines(path);

        double sum = 0;
        int count = 0;

        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split(",");
            int c = Integer.valueOf(parts[2]);
            double score = Double.valueOf(parts[8]);

            sum += c * score;
            count += c;
        }

        double avg = sum / count;
        listener.log(String.format("Average score: %.3f", avg));
    }

    @Override
    protected String getTaskClass() {
        return CLASS_EXPERIMENT;
    }

    @Override
    protected String getTaskTitle() {
        return "Calculate Weighted F1 Score";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        return new ArrayList<>();
    }
}
