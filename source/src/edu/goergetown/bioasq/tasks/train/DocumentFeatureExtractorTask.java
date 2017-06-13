package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.document.Sentence;
import edu.goergetown.bioasq.core.feature_extractor.DocumentFeatureExtractors;
import edu.goergetown.bioasq.core.feature_extractor.IDocumentFeatureExtractor;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by yektaie on 5/15/17.
 */
public class DocumentFeatureExtractorTask extends BaseTask {
    private IDocumentFeatureExtractor featureExtractor = null;

    private SubTaskInfo TASK_PROCESS = new SubTaskInfo("Extracting document features", 100);
    private SubTaskInfo TASK_PRE_PROCESS = new SubTaskInfo("Preprocessing feature extractor", 100);

    private DocumentFeatureExtractorInputType inputSource = DocumentFeatureExtractorInputType.TRAIN;

    public DocumentFeatureExtractorTask() {
        featureExtractor = DocumentFeatureExtractors.getFeatureExtractors().get(0);
    }

    @Override
    public void process(ITaskListener listener) {
        createFolder(featureExtractor);

        ArrayList<String> inputFiles = getInputFiles();
        featureExtractor.setInputFiles(inputFiles);

        if (featureExtractor.needPreprocessTask()) {
            featureExtractor.preprocess(listener);
        }

        ArrayList<ISubTaskThread> threads = splitTaskToThreads(featureExtractor, inputFiles);
        executeWorkerThreads(listener, TASK_PROCESS, threads);

        mergeFinalFilesIfTestOrDev(threads);
    }

    private void mergeFinalFilesIfTestOrDev(ArrayList<ISubTaskThread> threads) {
        if (inputSource.type != DocumentFeatureExtractorInputType.TYPE_TRAIN) {
            ArrayList<String> files = new ArrayList<>();
            for (ISubTaskThread thread : threads) {
                files.addAll(((DocumentFeatureExtractorThread)thread).finalMergedFiles);
            }

            String title = inputSource.title;
            title = title.substring(0, title.indexOf(" ")).toLowerCase();

            String path = inputSource.getBaseFolder() + featureExtractor.getDestinationFolderName() + Constants.BACK_SLASH + title + "_set.bin";
            DocumentFeatureSet.mergeFiles(path, files);

            for (String file : files) {
                FileUtils.delete(file);
            }
        }
    }

    private ArrayList<String> getInputFiles() {
        ArrayList<String> result = new ArrayList<>();

        if (inputSource.type == DocumentFeatureExtractorInputType.TYPE_TRAIN) {
            ArrayList<Integer> years = DocumentListUtils.getAvailableDocumentYears();
            for (int i = 0; i < Constants.CORE_COUNT; i++) {
                for (int year : years) {
                    String path = Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + i + ".bin";
                    result.add(path);
                }
            }
        } else if (inputSource.type == DocumentFeatureExtractorInputType.TYPE_DEV) {
            result.add(Constants.SETS_DATA_FOLDER + "dev_set.bin");
        } else if (inputSource.type == DocumentFeatureExtractorInputType.TYPE_TEST) {
            result.add(Constants.SETS_DATA_FOLDER + "test_set.bin");
        }

        return result;
    }

    private void createFolder(IDocumentFeatureExtractor featureExtractor) {
        String folder = inputSource.getBaseFolder() + featureExtractor.getDestinationFolderName() + Constants.BACK_SLASH;

        if (!FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }
    }

    private ArrayList<ISubTaskThread> splitTaskToThreads(IDocumentFeatureExtractor featureExtractor, ArrayList<String> inputFiles) {
        ArrayList<ISubTaskThread> result = new ArrayList<>();
        int threadCount = inputSource.getThreadCount();
        threadCount = Math.min(threadCount, featureExtractor.getCoreCount());

        for (int i = 0; i < threadCount; i++) {
            DocumentFeatureExtractorThread thread = new DocumentFeatureExtractorThread();

            thread.featureExtractor = featureExtractor;
            thread.threadID = i;
            thread.inputSource = inputSource;
            thread.includeMeSHListInFeatures = true;

            for (int j = 0; j < inputFiles.size(); j++) {
                if (threadCount == 1 || j % threadCount == i) {
                    if (FileUtils.exists(inputFiles.get(j))) {
                        thread.documentFiles.add(inputFiles.get(j));
                    }
                }
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

        ArrayList<Object> inputSources = new ArrayList<>();
        inputSources.add(DocumentFeatureExtractorInputType.TRAIN);
        inputSources.add(DocumentFeatureExtractorInputType.DEV);
        inputSources.add(DocumentFeatureExtractorInputType.TEST);

        result.put("extractor", extractors);
        result.put("sources", inputSources);

        return result;
    }

    @Override
    public Object getParameter(String name) {
        if (name.equals("extractor")) {
            return featureExtractor;
        } else if (name.equals("sources")) {
            return inputSource;
        }

        return null;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            if (name.equals("extractor")) {
                featureExtractor = (IDocumentFeatureExtractor) value;
            } else if (name.equals("sources")) {
                inputSource = (DocumentFeatureExtractorInputType) value;
            }
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
    public DocumentFeatureExtractorInputType inputSource = null;
    public ArrayList<String> finalMergedFiles = new ArrayList<>();
    public boolean includeMeSHListInFeatures = false;
    public StringBuilder contentAsText = new StringBuilder();

    public static boolean saveAsTextFile = true;

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

                    if (saveAsTextFile) {
                        contentAsText.append("pmid: " + document.identifier + ", year: " + document.metadata.get("year"));
                        contentAsText.append("\n");
                        contentAsText.append(document.title.toString(false, true));
                        contentAsText.append("\n");
                        for (Sentence sentence : document.text) {
                            contentAsText.append(sentence.toString(false, true));
                            contentAsText.append("\n");
                        }
                        contentAsText.append("\n");
                    }
                }
            });
        }

        if (saveAsTextFile) {
            FileUtils.writeText(Constants.TEMP_FOLDER + "Text Contents" + Constants.BACK_SLASH + "input-" + threadID + ".txt", contentAsText.toString());
        }

        if (processedCount > 0)
            saveProcessedDocuments();

        mergeFeatureSetFiles();

        finished = true;
    }

    private void mergeFeatureSetFiles() {
        for (int year : filesSaved.keySet()) {
            String path = inputSource.getBaseFolder() + featureExtractor.getDestinationFolderName() + Constants.BACK_SLASH + year + "-" + threadID + ".bin";
            DocumentFeatureSet.mergeFiles(path, filesSaved.get(year));
            finalMergedFiles.add(path);

            for (String filePath : filesSaved.get(year)) {
                FileUtils.delete(filePath);
            }
        }
    }

    private void processDocument(Document document) {
        int year = Integer.valueOf(document.metadata.get("year"));

        DocumentFeatureSet features = featureExtractor.extractFeatures(document);
        if (includeMeSHListInFeatures) {
            features.meshList = document.categories;
        }
        features.documentIdentifier = document.identifier;
        features.documentYear = year;

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
        String folder = inputSource.getBaseFolder() + featureExtractor.getDestinationFolderName() + Constants.BACK_SLASH;
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

class DocumentFeatureExtractorInputType {
    public static final int TYPE_TRAIN = 0;
    public static final int TYPE_DEV = 1;
    public static final int TYPE_TEST = 2;

    public String title = "";
    public int type = 0;

    public static DocumentFeatureExtractorInputType TRAIN = new DocumentFeatureExtractorInputType("Training Data", TYPE_TRAIN);
    public static DocumentFeatureExtractorInputType DEV = new DocumentFeatureExtractorInputType("Dev Data", TYPE_DEV);
    public static DocumentFeatureExtractorInputType TEST = new DocumentFeatureExtractorInputType("Test Data", TYPE_TEST);

    private DocumentFeatureExtractorInputType(String title, int type) {
        this.title = title;
        this.type = type;
    }

    @Override
    public String toString() {
        return title;
    }

    public String getBaseFolder() {
        String result = "";

        switch (type) {
            case DocumentFeatureExtractorInputType.TYPE_TRAIN:
                result = Constants.TRAIN_DOCUMENT_FEATURES_DATA_FOLDER;
                break;
            case DocumentFeatureExtractorInputType.TYPE_DEV:
                result = Constants.DEV_DOCUMENT_FEATURES_DATA_FOLDER;
                break;
            case DocumentFeatureExtractorInputType.TYPE_TEST:
                result = Constants.TEST_DOCUMENT_FEATURES_DATA_FOLDER;
                break;
        }

        return result;
    }

    public int getThreadCount() {
        int result = 1;

        if (type == TYPE_TRAIN) {
            result = Constants.CORE_COUNT;
        }

        return result;
    }
}