package edu.goergetown.bioasq.tasks.preprocess;

import com.google.gson.Gson;
import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.BioAsqEntry;
import edu.goergetown.bioasq.core.task.*;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class ExtractDatasetOnPaperYearTask extends BaseTask {
    private SubTaskInfo EXTRACTION_TASK = new SubTaskInfo("Extraction from original file", 12);
//    private SubTaskInfo MERGING_TASK = new SubTaskInfo("Merging intermediate temp files", 5);

    @Override
    public void process(ITaskListener listener) {
        final int countOfDocuments = 12834585;
        listener.setCurrentState("Loading mesh JSON file");
        Hashtable<Integer, ArrayList<BioAsqEntry>> entries = new Hashtable<>();
        int count = 0;
        int countForMerge = 10000;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(Constants.ORIGINAL_DATA_FILE), "UTF8"));

            String line = null;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("{\"journal")) {
                    if (line.endsWith("}]}")) {
                        line = line.replace("}]}", "}");
                    } else {
                        line = line.substring(0, line.length() - 1);
                    }

                    Gson gson = new Gson();
                    BioAsqEntry entry = gson.fromJson(line, BioAsqEntry.class);
                    if (entries.containsKey(entry.year)) {
                        entries.get(entry.year).add(entry);
                    } else {
                        ArrayList<BioAsqEntry> list = new ArrayList<>();
                        list.add(entry);

                        entries.put(entry.year, list);
                    }

                    if (count % 1000 == 0) {
                        updateProgress(listener, EXTRACTION_TASK, count, countOfDocuments);
                    }

                    count++;

                    if (count % countForMerge == 0) {
                        saveEntries(entries, count / countForMerge);
                        entries.clear();

//                        listener.log("Entries saved ...");
                    }
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        saveEntries(entries, count / countForMerge);
//        mergeEntries(listener);
    }

    private void saveEntries(Hashtable<Integer, ArrayList<BioAsqEntry>> entries, int index) {
        for (int key : entries.keySet()) {
            ArrayList<BioAsqEntry> list = entries.get(key);
            StringBuilder file = new StringBuilder();

            for (BioAsqEntry entry : list) {
                file.append(entry.serialize());
                file.append("\n----------------------------------\n");
            }

            if (!FileUtils.exists(Constants.getDataFolder(key))) {
                FileUtils.createDirectory(Constants.getDataFolder(key));
            }

            String path = String.format("%sentries-%d.txt", Constants.getDataFolder(key), index);
            FileUtils.writeText(path, file.toString());
        }

        entries.clear();
    }

    @Override
    protected String getTaskClass() {
        return CLASS_PREPROCESS;
    }

    @Override
    protected String getTaskTitle() {
        return "Extract Papers Based on Publication Year";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(EXTRACTION_TASK);
//        result.add(MERGING_TASK);

        return result;
    }
}
