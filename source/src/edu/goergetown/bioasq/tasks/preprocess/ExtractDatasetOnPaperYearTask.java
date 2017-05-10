package edu.goergetown.bioasq.tasks.preprocess;

import com.google.gson.Gson;
import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.BaseTask;
import edu.goergetown.bioasq.core.BioAsqEntry;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class ExtractDatasetOnPaperYearTask extends BaseTask {
    @Override
    public void process(ITaskListener listener) {
        final int countOfDocuments = 12834585;
        listener.setCurrentState("Loading mesh JSON file");
        Hashtable<Integer, Integer> lastIndex = new Hashtable<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(Constants.ORIGINAL_DATA_FILE), "UTF8"));

            String line = null;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("{\"journal")) {
                    if (line.endsWith("}]}")) {
                        line = line.replace("}]}", "}");
                    } else {
                        line = line.substring(0, line.length() - 1);
                    }

                    Gson gson = new Gson();
                    BioAsqEntry entry = gson.fromJson(line, BioAsqEntry.class);
                    entry.save(getFilePath(entry, lastIndex));

                    if (count % 1000 == 0) {
                        listener.setProgress(count, countOfDocuments);
                    }

                    count++;
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getFilePath(BioAsqEntry entry, Hashtable<Integer, Integer> lastIndex) {
        int index = 0;

        if (lastIndex.containsKey(entry.year)) {
            index = lastIndex.get(entry.year) + 1;

            if (index % Constants.NUMBER_OF_FILE_IN_DATASET_FOLDER == 0) {
                String path = String.format("%s%s%s",Constants.getDataFolder(entry.year),String.valueOf(index / Constants.NUMBER_OF_FILE_IN_DATASET_FOLDER),Constants.BACK_SLASH);

                if (!FileUtils.exists(path)) {
                    FileUtils.createDirectory(path);
                }
            }

            lastIndex.put(entry.year, index);
        } else {
            String path = String.format("%s%s%s",Constants.getDataFolder(entry.year),"0",Constants.BACK_SLASH);

            if (!FileUtils.exists(path)) {
                FileUtils.createDirectory(path);
            }

            lastIndex.put(entry.year, 0);
        }

        return String.format("%s%s%s%d%s",Constants.getDataFolder(entry.year),String.valueOf(index / Constants.NUMBER_OF_FILE_IN_DATASET_FOLDER),Constants.BACK_SLASH, index, ".txt");
    }

    @Override
    protected String getTaskClass() {
        return CLASS_PREPROCESS;
    }

    @Override
    protected String getTaskTitle() {
        return "Extract Papers Based on Publication Year";
    }
}
