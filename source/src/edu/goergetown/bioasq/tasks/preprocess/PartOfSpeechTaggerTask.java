package edu.goergetown.bioasq.tasks.preprocess;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.BioAsqEntry;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/11/2017.
 */
public class PartOfSpeechTaggerTask extends BaseTask {
    private SubTaskInfo LOADER_TASK = new SubTaskInfo("Loading BioASQ entries", 1);
    private SubTaskInfo TAGGER_TASK = new SubTaskInfo("Part of Speech Tagging entries", 935);
    private int partToRun = 3;

    @Override
    public void process(ITaskListener listener) {
        ArrayList<BioAsqEntry> entries = loadEntries(listener);

        entries = less(entries);

        convertEntries(listener, entries);
        mergeEntries(listener);
    }

    private void mergeEntries(ITaskListener listener) {
        ArrayList<Document> documents = new ArrayList<>();

        for (int i = 0; i < 21; i++) {
            listener.log("Processing part " + i + " for merge");
            for (int j = 0; j < 4; j++) {
                String path = Constants.POS_DATA_FOLDER_BY_YEAR + 2015 + "-" + j+ "-" + i + ".bin";
                for(Document d : Document.loadListFromFile(path)) {
                    documents.add(d);
                }
            }
        }

        listener.log("Merging all files");
        String path = Constants.POS_DATA_FOLDER_BY_YEAR + 2015 + "-" + partToRun+".bin";
        Document.saveList(path, documents);
    }

    private ArrayList<BioAsqEntry> less(ArrayList<BioAsqEntry> entries) {
        ArrayList<BioAsqEntry> result = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            if ( i%4 == partToRun) {
                result.add(entries.get(i));
            }
        }

        return result;
    }

    private void convertEntries(ITaskListener listener, ArrayList<BioAsqEntry> entries) {
        Document.fromBioAsq(entries.get(0));
        ArrayList<ArrayList<BioAsqEntry>> parts = splitParts(entries, Constants.CORE_COUNT);
        ArrayList<ISubTaskThread> threads = createPartOfSpeechWorkerThreads(parts, Constants.CORE_COUNT);

        listener.log("Start POS Tagger Threads");
        executeWorkerThreads(listener, TAGGER_TASK, threads);
        listener.log("POS Tagger Threads Finished");
    }

    private ArrayList<ISubTaskThread> createPartOfSpeechWorkerThreads(ArrayList<ArrayList<BioAsqEntry>> parts, int coreCount) {
        ArrayList<ISubTaskThread> result = new ArrayList<>();

        for (int i = 0; i < coreCount; i++) {
            PartOfSpeechTaggerThread t = new PartOfSpeechTaggerThread();
            t.entries = parts.get(i);
            t.outputFilePath = Constants.POS_DATA_FOLDER_BY_YEAR;
            t.threadIndex = i;

            result.add(t);
        }

        return result;
    }

    private ArrayList<ArrayList<BioAsqEntry>> splitParts(ArrayList<BioAsqEntry> entries, int coreCount) {
        ArrayList<ArrayList<BioAsqEntry>> result = new ArrayList<>();

        for (int i = 0; i < coreCount; i++) {
            result.add(new ArrayList<>());
        }

        for (int i = 0; i < entries.size(); i++) {
            result.get(i % coreCount).add(entries.get(i));
        }

        return result;
    }

    private ArrayList<BioAsqEntry> loadEntries(ITaskListener listener) {
        ArrayList<BioAsqEntry> result = new ArrayList<>();
        ArrayList<String> files= loadInputFilesList();

        for (int i = 0; i < files.size(); i++) {
            StringBuilder lines = new StringBuilder();

            try {
                InputStream fileStream = new FileInputStream(files.get(i));
                int fileSize = fileStream.available();
                BufferedReader br = new BufferedReader(new InputStreamReader(fileStream, "UTF8"));
                String line = null;
                int read = 0;
                int count = 0;

                while ((line = br.readLine()) != null) {
                    read = read + 1 + line.length();
                    count++;
                    if (count % 1000 == 0) {
                        updateProgress(listener, LOADER_TASK, read, fileSize);
                    }

                    if (line.equals("----------------------------------")) {
                        addEntry(result, lines.toString());
                        lines = new StringBuilder();
                    } else {
                        lines.append(line);
                        lines.append("\n");
                    }
                }

                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            addEntry(result, lines.toString());
        }

        return result;
    }

    private void addEntry(ArrayList<BioAsqEntry> result, String data) {
        BioAsqEntry entry = BioAsqEntry.load(data);

        if (entry != null) {
            result.add(entry);
        }
    }

    private ArrayList<String> loadInputFilesList() {
        return FileUtils.getFiles(Constants.DATA_FOLDER_BY_YEAR);
    }

    @Override
    protected String getTaskClass() {
        return CLASS_PREPROCESS;
    }

    @Override
    protected String getTaskTitle() {
        return "Part of Speech Tagger";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(LOADER_TASK);
        result.add(TAGGER_TASK);

        return result;
    }
}

class PartOfSpeechTaggerThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    public String outputFilePath = "";
    public ArrayList<BioAsqEntry> entries = null;
    public ArrayList<Document> results = new ArrayList<>();
    private double progress = 0;
    public int threadIndex = 0;

    @Override
    public void run() {
        int count = entries.size();
        int saveIndex = 0;

        int processed = 0;
        while (entries.size() > 0) {
            progress = processed * 100.0 / count;

            results.add(Document.fromBioAsq(entries.get(0)));
            entries.remove(0);

            if (processed == 50 || processed % 2000 == 0) {
                saveResultsInFiles(saveIndex);
                saveIndex++;

                results.clear();
            }

            processed++;
        }

        saveResultsInFiles(saveIndex);
        results.clear();

        finished = true;
    }

    private void saveResultsInFiles(int saveIndex) {
        Hashtable<Integer, ArrayList<Document>> splitOnYears = splitDocumentsOnYear();
        for (int year : splitOnYears.keySet()) {
            saveFile(splitOnYears.get(year), year,saveIndex);
        }
    }

    private void saveFile(ArrayList<Document> documents, int year, int saveIndex) {
        String path = outputFilePath + year + "-" + threadIndex+ "-" + saveIndex + ".bin";

        Document.saveList(path, documents);
    }

    private Hashtable<Integer, ArrayList<Document>> splitDocumentsOnYear() {
        Hashtable<Integer, ArrayList<Document>> result = new Hashtable<>();

        for (Document doc : results) {
            int year = Integer.valueOf(doc.metadata.get("year"));
            if (result.containsKey(year)) {
                result.get(year).add(doc);
            } else {
                ArrayList<Document> list = new ArrayList<>();
                list.add(doc);

                result.put(year, list);
            }
        }

        return result;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public double getProgress() {
        return progress;
    }
}
