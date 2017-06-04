package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.Guid;
import edu.goergetown.bioasq.core.document.IDocumentFeatureSetLoaderCallback;
import edu.goergetown.bioasq.core.document.Stemmer;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import static edu.stanford.nlp.util.ArrayCoreMap.listener;

/**
 * Created by Yektaie on 6/1/2017.
 */
public class MeSHTermInDocumentTextClassifierTrainerTask extends BaseTask {
    private static final double THRESHOLD = 0.3;
    private SubTaskInfo LOADING_DOCUMENTS = new SubTaskInfo("Loading documents", 0);
    private SubTaskInfo EVALUATE_CLASSIFIER = new SubTaskInfo("Evaluating classifiers", 10);

    private String featureMethod = null;

    public MeSHTermInDocumentTextClassifierTrainerTask() {
        featureMethod = "bm25f";
    }

    @Override
    public void process(ITaskListener listener) {
        if (!FileUtils.exists(getMeSHListFilePath())) {
            listener.log("Loading documents");
            ArrayList<DocumentFeatureSet> featureSets = loadDocuments(featureMethod, listener);
            getMeshList(featureSets);
            splitDocumentsForParallelization(featureSets);

            listener.log("Every thing is ready for the main run. Please run this task again.");
        } else {
            ArrayList<ISubTaskThread> threads = createWorkerThreads();
            executeWorkerThreads(listener, EVALUATE_CLASSIFIER, threads);

            saveResults(((TextClassifierThread)threads.get(0)).meshList);
        }

    }

    private ArrayList<ISubTaskThread> createWorkerThreads() {
        ArrayList<ISubTaskThread> result = new ArrayList<>();
        Hashtable<String, EvaluationResult> meshList = loadMeshList();

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            TextClassifierThread t = new TextClassifierThread();

            t.inputFilePath = getTempFolder() + "part-" + i + ".bin";
            t.meshList = cloneMeshList(meshList);
            t.threshold= THRESHOLD;

            result.add(t);
        }

        return result;
    }

    private Hashtable<String, EvaluationResult> loadMeshList() {
        Hashtable<String, EvaluationResult> result = new Hashtable<>();

        String[] lines = FileUtils.readAllLines(getMeSHListFilePath());
        for (int i = 0; i < lines.length; i++) {
            String mesh = lines[i];

            EvaluationResult r = new EvaluationResult();
            r.mesh = mesh;

            result.put(mesh, r);
        }

        return result;
    }

    private Hashtable<String, EvaluationResult> cloneMeshList(Hashtable<String, EvaluationResult> meshList) {
        Hashtable<String, EvaluationResult> result = new Hashtable<>();

        result.putAll(meshList);

        return result;
    }

    private void splitDocumentsForParallelization(ArrayList<DocumentFeatureSet> featureSets) {
        String folder = getTempFolder();
        if (!FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }

        ArrayList<ArrayList<DocumentFeatureSet>> lists = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            lists.add(new ArrayList<>());
        }

        for (int i = 0; i < featureSets.size(); i++) {
            lists.get(i % Constants.CORE_COUNT).add(featureSets.get(i));
        }

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            DocumentFeatureSet.saveList(folder + "part-" + i + ".bin", lists.get(i));
        }
    }

    private String getTempFolder() {
        return Constants.TEMP_FOLDER + "Text Classifier" + Constants.BACK_SLASH + "MeSH Term In Document" + Constants.BACK_SLASH + featureMethod + Constants.BACK_SLASH;
    }

    private void saveResults(Hashtable<String, EvaluationResult> meshList) {
        EvaluationResult[] entries = new EvaluationResult[meshList.size()];
        int i = 0;
        for (String mesh : meshList.keySet()) {
            entries[i] = meshList.get(mesh);
            i++;
        }

        Arrays.sort(entries);

        StringBuilder result = new StringBuilder();
        result.append("ID,MeSH,Document Count,Correct Prediction,True Negative,False Positive,Precision,Recall,F1");
        int id = 0;
        for (EvaluationResult entry : entries) {
            id++;
            result.append(String.format("\r\n%d,%s,%d,%d,%d,%d,%.3f,%.3f,%.3f", id, entry.mesh.replace(",", "&coma;"), entry.documentContainingLabel, entry.correctPrediction, entry.trueNegative, entry.falsePositive, entry.getPrecision(), entry.getRecall(), entry.getScore()));
        }
        FileUtils.writeText(Constants.TEMP_FOLDER + "text-classification-mesh-term-" + featureMethod + ".csv", result.toString());
    }

    private Hashtable<String, EvaluationResult> getMeshList(ArrayList<DocumentFeatureSet> vectors) {
        Hashtable<String, EvaluationResult> result = new Hashtable<>();
        StringBuilder meshes = new StringBuilder();

        for (DocumentFeatureSet document : vectors) {
            for (String mesh : document.meshList) {
                EvaluationResult r = null;

                if (!result.containsKey(mesh)) {
                    r = new EvaluationResult();
                    r.mesh = mesh;
                    result.put(mesh, r);
                    meshes.append(mesh);
                    meshes.append("\r\n");
                }

                r = result.get(mesh);
                r.documentContainingLabel++;
            }
        }

        FileUtils.writeText(getMeSHListFilePath(), meshes.toString());

        return result;
    }

    public static String getMeSHListFilePath() {
        return Constants.TEMP_FOLDER + "mesh-list-training-set.txt";
    }

    private ArrayList<DocumentFeatureSet> loadDocuments(String featureMethod, ITaskListener listener) {
        ArrayList<String> files = FileUtils.getFiles(Constants.TRAIN_DOCUMENT_FEATURES_DATA_FOLDER + featureMethod + Constants.BACK_SLASH);
        ArrayList<DocumentFeatureSet> result = new ArrayList<>();

        for (String file : files) {
            ArrayList<DocumentFeatureSet> list = DocumentFeatureSet.loadListFromFile(listener, file);
            result.addAll(list);

            listener.log("");
        }

        return result;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Text Classifier Trainer: MeSH Term in Document";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(LOADING_DOCUMENTS);
        result.add(EVALUATE_CLASSIFIER);

        return result;
    }

    @Override
    public Object getParameter(String name) {
        return featureMethod;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            if (name.equals("feature_method")) {
                featureMethod = (String) value;
            }
        }
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        ArrayList<Object> configs = new ArrayList<>();
        configs.add("bm25f");
        configs.add("tf-idf");
        configs.add("topic-rank");
        configs.add("umls");

        result.put("feature-method", configs);

        return result;
    }

}

class EvaluationResult implements Comparable<EvaluationResult> {
    public int documentContainingLabel = 0;
    public int correctPrediction = 0;
    public int trueNegative = 0;
    public int falsePositive = 0;
    public String mesh = "";

    public double getScore() {
        double p = getPrecision();
        double r = getRecall();

        if ((p + r) == 0) {
            return 0;
        }

        return 2 * (r * p)/ (r + p);
    }

    public double getRecall() {
        if (documentContainingLabel == 0)
            return 0;

        return correctPrediction * 1.0 / documentContainingLabel;
    }

    public double getPrecision() {
        if ((correctPrediction + falsePositive) == 0) {
            return 0;
        }

        return correctPrediction * 1.0 / (correctPrediction + falsePositive);
    }

    @Override
    public int compareTo(EvaluationResult o) {
        return new Double(o.getScore()).compareTo(getScore());
    }
}

class TextClassifierThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0;
    public String inputFilePath = "";
    private Stemmer stemmer = new Stemmer();
    public Hashtable<String, EvaluationResult> meshList = null;
    public double threshold = 0;

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void run() {
        DocumentFeatureSet.iterateOnDocumentFeatureSetListFile(inputFilePath, new IDocumentFeatureSetLoaderCallback() {
            @Override
            public boolean processDocumentFeatureSet(DocumentFeatureSet document, int i, int totalDocumentCount) {
                progress = i * 100.0 / totalDocumentCount;
                ArrayList<String> processedFeatures = getProcessedFeatures(document);

                for (String mesh : meshList.keySet()) {
                    boolean classificationResult = classifyDocument(document, processedFeatures, mesh);
                    boolean actualResult = document.meshList.contains(mesh);

                    EvaluationResult entry = meshList.get(mesh);
                    synchronized (entry) {
                        if (actualResult && classificationResult) {
                            entry.correctPrediction++;
                            entry.documentContainingLabel++;
                        } else if (actualResult && !classificationResult) {
                            entry.trueNegative++;
                            entry.documentContainingLabel++;
                        } else if (!actualResult && classificationResult) {
                            entry.falsePositive++;
                        }
                    }
                }

                return true;
            }
        });

        finished = true;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    private boolean classifyDocument(DocumentFeatureSet document, ArrayList<String> processedFeatures, String mesh) {
        String[] lookingForFeatures = replaceChars(mesh).split(" ");

        int count = 0;
        for (String feature : lookingForFeatures) {
            if (processedFeatures.contains(stemmer.stem(feature)))
                count++;
        }

        return (count * 1.0 / lookingForFeatures.length) > threshold;
    }

    private ArrayList<String> getProcessedFeatures(DocumentFeatureSet document) {
        ArrayList<String> temp = new ArrayList<>();
        for (String feature : document.features.keySet()) {
            String f = stemmer.stem(feature.toLowerCase());
            temp.add(f);
        }
        return temp;
    }

    private String replaceChars(String mesh) {
        return mesh.replace(",", " ").toLowerCase();
    }

}