package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.feature_extractor.DocumentFeatureExtractors;
import edu.goergetown.bioasq.core.feature_extractor.IDocumentFeatureExtractor;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by yektaie on 5/15/17.
 */
public class DocumentFeatureExtractorTask extends BaseTask {
    private IDocumentFeatureExtractor featureExtractor = null;

    private SubTaskInfo TASK_PROCESS = new SubTaskInfo("Extracting document features", 100);
    private SubTaskInfo TASK_PRE_PROCESS = new SubTaskInfo("Preprocessing feature extractor", 100);

    public DocumentFeatureExtractorTask() {
        featureExtractor = DocumentFeatureExtractors.getFeatureExtractors().get(0);
    }

    @Override
    public void process(ITaskListener listener) {
        ArrayList<Integer> years = DocumentListUtils.getAvailableDocumentYears();
        createFolder(featureExtractor);

        if (featureExtractor.needPreprocessTask()) {
            featureExtractor.prepreocess(listener);
        }

        ArrayList<ISubTaskThread> thread = splitTaskToThreads(years, featureExtractor);
        executeWorkerThreads(listener, TASK_PROCESS, thread);

        if (featureExtractor.needNormalizationOfFeatures()){
            featureExtractor.normalizeFeatures(listener, null, null);
        }
    }

    private void createFolder(IDocumentFeatureExtractor featureExtractor) {
        if (!FileUtils.exists(featureExtractor.getDestinationFolder())) {
            FileUtils.createDirectory(featureExtractor.getDestinationFolder());
        }
    }

    private ArrayList<ISubTaskThread> splitTaskToThreads(ArrayList<Integer> years, IDocumentFeatureExtractor featureExtractor) {
        ArrayList<ISubTaskThread> result = new ArrayList<>();

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            DocumentFeatureExtractorThread thread = new DocumentFeatureExtractorThread();

            thread.featureExtractor = featureExtractor;
            thread.threadID = i;
            for (int year : years) {
                thread.documentFiles.add(Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + i + ".bin");
            }

            result.add(thread);
        }

        return result;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Document Feature Extractor";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        if (featureExtractor.needPreprocessTask()) {
            TASK_PRE_PROCESS.estimation = featureExtractor.getPreprocessTaskGetTimeInMinutes();
            result.add(TASK_PRE_PROCESS);
        }

        result.add(TASK_PROCESS);

        return result;
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();

        ArrayList<Object> extractors = new ArrayList<>();
        extractors.addAll(DocumentFeatureExtractors.getFeatureExtractors());

        result.put("extractor", extractors);

        return result;
    }

    @Override
    public Object getParameter(String name) {
        if (name.equals("extractor")) {
            return featureExtractor;
        }

        return null;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (name.equals("extractor")) {
            featureExtractor = (IDocumentFeatureExtractor) value;
        }
    }
}

class DocumentFeatureExtractorThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0.0;
    public ArrayList<String> documentFiles = new ArrayList<>();
    public IDocumentFeatureExtractor featureExtractor = null;

    private Hashtable<Integer, ArrayList<DocumentFeatureSet>> processedDocuments = new Hashtable<>();
    public int threadID = 0;
    private int savedTimeCount = 0;
    private int processedCount = 0;
    private Hashtable<Integer, ArrayList<String>> filesSaved = new Hashtable<>();

    @Override
    public void run() {
        int totalDocumentToProcess = getTotalDocumentToProcessCount();
        final int[] processedDocumentCount = {0};

        for (String documentListFilePath : documentFiles) {
            Document.iterateOnDocumentListFile(documentListFilePath, new IDocumentLoaderCallback() {
                @Override
                public void processDocument(Document document, int i, int totalDocumentCount) {
                    updateProgress(processedDocumentCount, totalDocumentToProcess);
                    DocumentFeatureExtractorThread.this.processDocument(document);
                }
            });
        }

        if (processedCount > 0)
            saveProcessedDocuments();

        mergeFeatureSetFiles();

        finished = true;
    }

    private void mergeFeatureSetFiles() {
        for (int year : filesSaved.keySet()) {
            String path = featureExtractor.getDestinationFolder() + year + "-" + threadID + ".bin";
            DocumentFeatureSet.mergeFiles(path, filesSaved.get(year));

            for (String filePath : filesSaved.get(year)) {
                FileUtils.delete(filePath);
            }
        }
    }

    private void processDocument(Document document) {
        DocumentFeatureSet features = featureExtractor.extractFeatures(document);

        int year = Integer.valueOf(document.metadata.get("year"));
        if (processedDocuments.containsKey(year)) {
            processedDocuments.get(year).add(features);
        } else {
            processedDocuments.put(year, new ArrayList<>());
            processedDocuments.get(year).add(features);
        }

        processedCount++;

        if (processedCount == 10000) {
            saveProcessedDocuments();
            processedDocuments.clear();
            processedCount = 0;
        }
    }

    private void saveProcessedDocuments() {
        String folder = featureExtractor.getDestinationFolder();
        for (int year : processedDocuments.keySet()) {
            String path = folder + year + "-" + threadID + "-" + savedTimeCount + ".bin";
            ArrayList<DocumentFeatureSet> list = processedDocuments.get(year);

            DocumentFeatureSet.saveList(path, list);
            if (!filesSaved.containsKey(year)) {
                filesSaved.put(year, new ArrayList<>());
            }

            filesSaved.get(year).add(path);
        }
        savedTimeCount++;
    }

    private void updateProgress(int[] processedDocumentCount, int totalDocumentToProcess) {
        progress = processedDocumentCount[0] * 100.0 / totalDocumentToProcess;
        processedDocumentCount[0]++;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    public int getTotalDocumentToProcessCount() {
        int result = 0;

        for (String path : documentFiles) {
            result += Document.getDocumentCountInFile(path);
        }

        return result;
    }
}