package edu.goergetown.bioasq.tasks.evaluation;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassifierParameter;
import edu.goergetown.bioasq.core.model.classification.MeSHClassificationModelBase;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.core.predictor.MeshPredictors;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/30/2017.
 */
public class EvaluationTask extends BaseTask {
    private SubTaskInfo EVALUATE_TASK = new SubTaskInfo("Evaluating data set", 200);
    private ClassifierParameter parameter = null;
    private EvaluationInputSource inputSource = EvaluationInputSource.DEV;
    private ArrayList<ClassifierParameter> configurations = new ArrayList<>();
    private IMeshPredictor predictor = null;

    public EvaluationTask() {
        ArrayList<ClassifierParameter> configurations = ClassifierParameter.getConfigurations();
        for (ClassifierParameter p : configurations) {
            if (MeSHClassificationModelBase.exists(p)) {
                this.configurations.add(p);
            }
        }

        if (this.configurations.size() > 0) {
            parameter = this.configurations.get(0);
        }

        ArrayList<IMeshPredictor> list = MeshPredictors.getPredictors();
        predictor = list.get(0).duplicate();
//        predictor = list.get(list.size() - 1).duplicate();
    }

    @Override
    public void process(ITaskListener listener) {
        listener.log("Load model MD5 hash");
        String modelHash = MeSHClassificationModelBase.getModelHash(listener, parameter);
        listener.log(getCacheFilePath(modelHash));
        listener.log("Load model clusters");
        ArrayList<ClassificationCluster> model = MeSHClassificationModelBase.load(listener, parameter);

        listener.log("Loading set document features");
        ArrayList<DocumentFeatureSet> documents = DocumentFeatureSet.loadListFromFile(inputSource.getFeatureSetFolder() + parameter.termExtractionMethod + Constants.BACK_SLASH + inputSource.getFeatureSetFileName());

        listener.log("Classifying documents into clusters");
        ArrayList<ISubTaskThread> threads = createWorkerThreads(model, modelHash, documents);
        executeWorkerThreads(listener, EVALUATE_TASK, threads);

        saveReport(threads, parameter, listener);

        if (!existsCacheFile(modelHash)) {
            listener.log("Saving cluster map cache file");
            listener.setCurrentState("Saving cluster map cache file");
            Hashtable<String, ArrayList<String>> cachedClusterMaps = getClusterMaps(threads);
            saveCacheFile(listener, modelHash, cachedClusterMaps);
        }

    }

    private void saveReport(ArrayList<ISubTaskThread> threads, ClassifierParameter parameter, ITaskListener listener) {
        ArrayList<DocumentEvaluationResult> results = new ArrayList<>();
        for (int i = 0; i < threads.size(); i++) {
            EvaluationThread t = (EvaluationThread) threads.get(i);
            results.addAll(t.results);
        }

        String reportFolder = Constants.REPORTS_FOLDER + parameter.toString() + Constants.BACK_SLASH + predictor.getReportFolderName() + Constants.BACK_SLASH;
        if (!FileUtils.exists(reportFolder)) {
            FileUtils.createDirectory(reportFolder);
        }

        saveCSVReport(results, reportFolder, listener);
    }

    private void saveCSVReport(ArrayList<DocumentEvaluationResult> results, String folder, ITaskListener listener) {
        StringBuilder result = new StringBuilder();
        result.append("Document Identifier,Precision,Recall,F1 Score");

        double avgPrecision = 0;
        double avgRecall = 0;
        double avgF1 = 0;

        for (DocumentEvaluationResult entry : results) {
            result.append(String.format("\r\n%s,%.5f,%.5f,%.5f", entry.documentIdentifier, entry.precision, entry.recall, entry.f1));

            avgPrecision += entry.precision;
            avgRecall += entry.recall;
            avgF1 += entry.f1;
        }

        listener.log("");
        listener.log("Evaluation Report:");
        listener.log(String.format("     Precision: %.3f", avgPrecision / results.size()));
        listener.log(String.format("     Recall: %.3f", avgRecall / results.size()));
        listener.log(String.format("     F1: %.3f", avgF1 / results.size()));
        listener.log("");

        FileUtils.writeText(folder + "Predictions Metrics.csv", result.toString());
    }

    private void saveCacheFile(ITaskListener listener, String modelHash, Hashtable<String, ArrayList<String>> cachedClusterMaps) {
        StringBuilder result = new StringBuilder();

        String del = "";
        int count = 0;
        int showUpdate = cachedClusterMaps.size() / 100;
        for (String key : cachedClusterMaps.keySet()) {
            if (count % showUpdate == 0)
                listener.setProgress(count, cachedClusterMaps.size());

            count++;
            result.append(del);
            result.append(key);
            for (String id : cachedClusterMaps.get(key)) {
                result.append("\r\n\t");
                result.append(id);
            }

            result.append("\r\n");
            del = "\r\n";
        }

        FileUtils.writeText(getCacheFilePath(modelHash), result.toString());
    }

    private boolean existsCacheFile(String modelHash) {
        String folder = Constants.TEMP_FOLDER + "Caches" + Constants.BACK_SLASH;
        if (!FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }

        String path = getCacheFilePath(modelHash);
        return FileUtils.exists(path);
    }

    private String getCacheFilePath(String modelHash) {
        String folder = Constants.TEMP_FOLDER + "Caches" + Constants.BACK_SLASH;
        StringBuilder fileName = new StringBuilder();
        for (int i = 0; i < modelHash.length(); i++) {
            char c = modelHash.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                fileName.append(c);
            }
        }
        return folder + fileName + ".txt";
    }

    private Hashtable<String, ArrayList<String>> getClusterMaps(ArrayList<ISubTaskThread> threads) {
        Hashtable<String, ArrayList<String>> result = new Hashtable<>();

        for (int i = 0; i < threads.size(); i++) {
            EvaluationThread t = (EvaluationThread) threads.get(i);
            result.putAll(t.mappedDocument);
        }

        return result;
    }

    private ArrayList<ISubTaskThread> createWorkerThreads(ArrayList<ClassificationCluster> model, String modelHash, ArrayList<DocumentFeatureSet> documents) {
        ArrayList<ISubTaskThread> result = new ArrayList<>();

        ArrayList<ArrayList<DocumentFeatureSet>> documentsList = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            documentsList.add(new ArrayList<>());
        }

        for (int i = 0; i < documents.size(); i++) {
            documentsList.get(i % Constants.CORE_COUNT).add(documents.get(i));
        }

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            EvaluationThread t = new EvaluationThread();

            t.modelClusters = model;
            t.modelHash = modelHash;
            t.documents = documentsList.get(i);
            t.predictor = predictor;
            t.mappedDocument = loadCache(modelHash);

            result.add(t);
        }

        return result;
    }

    private Hashtable<String, ArrayList<String>> loadCache(String modelHash) {
        Hashtable<String, ArrayList<String>> result = new Hashtable<>();

        if (FileUtils.exists(getCacheFilePath(modelHash))) {
            String id = null;
            ArrayList<String> list = null;

            String[] lines = FileUtils.readAllLines(getCacheFilePath(modelHash));

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (line.startsWith("\t")) {
                    list.add(line.trim());
                } else if (!line.trim().equals("")) {
                    id = line.trim();
                    list = new ArrayList<>();

                    result.put(id, list);
                }
            }
        }

        return result;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_CLASSIFIER;
    }

    @Override
    protected String getTaskTitle() {
        return "Evaluate Development Set";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(EVALUATE_TASK);

        return result;
    }

    @Override
    public Object getParameter(String name) {
        return parameter;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            if (name.equals("configurations")) {
                parameter = (ClassifierParameter) value;
            } else if (name.equals("input")) {
                inputSource = (EvaluationInputSource) value;
            }
        }
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        ArrayList<Object> configs = new ArrayList<>();
        configs.addAll(configurations);

        ArrayList<Object> input = new ArrayList<>();
        input.add(EvaluationInputSource.DEV);
        input.add(EvaluationInputSource.TEST);

        result.put("configurations", configs);
        result.put("input", input);

        return result;
    }

}

class EvaluationThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0;
    public ArrayList<ClassificationCluster> modelClusters = null;
    public String modelHash = "";
    public ArrayList<DocumentFeatureSet> documents = null;
    public Hashtable<String, ArrayList<String>> mappedDocument = new Hashtable<>();
    public ArrayList<DocumentEvaluationResult> results = new ArrayList<>();
    public IMeshPredictor predictor = null;
    public int neiborsToConsider = 5;

    @Override
    public void run() {
//        for (int i = 0; i < 100; i++) {
        for (int i = 0; i < documents.size(); i++) {
            updateProgress(i);

            DocumentFeatureSet document = documents.get(i);
            Vector vector = document.toVector();

            ArrayList<ClassificationCluster> clusters = getClassificationClusters(vector);
            cacheClusterMap(vector, clusters);

            ArrayList<String> predictions = new ArrayList<>();
            for (ClassificationCluster cluster : clusters) {
                ArrayList<String> predictedMesh = predictor.predict(document, cluster);
                predictions.addAll(predictedMesh);
            }

            predictions = removeDuplicates(predictions);

            ArrayList<String> correctMesh = document.meshList;
            ArrayList<String> correctPredictions = getCorrectPredictions(predictions, correctMesh);

            DocumentEvaluationResult result = new DocumentEvaluationResult();

            if (predictions.size() != 0) {
                result.precision = correctPredictions.size() * 1.0 / predictions.size();
                result.recall = correctPredictions.size() * 1.0 / correctMesh.size();
                result.f1 = 2 * (result.precision * result.recall) / (result.precision + result.recall);
            }

            result.correctResults = correctMesh;
            result.predictedResults = predictions;
            result.documentIdentifier = vector.identifier;

            results.add(result);
        }

        finished = true;
    }

    private ArrayList<String> removeDuplicates(ArrayList<String> predictions) {
        ArrayList<String> result = new ArrayList<>();

        for (String mesh : predictions) {
            if (!result.contains(mesh)) {
                result.add(mesh);
            }
        }

        return result;
    }

    private ArrayList<String> getCorrectPredictions(ArrayList<String> predictedMesh, ArrayList<String> correctMesh) {
        ArrayList<String> result = new ArrayList<>();

        for (String predicted : predictedMesh) {
            if (correctMesh.contains(predicted)) {
                result.add(predicted);
            }
        }

        return result;
    }

    private void cacheClusterMap(Vector vector, ArrayList<ClassificationCluster> clusters) {
        ArrayList<String> ids = new ArrayList<>();
        for (ClassificationCluster cluster : clusters) {
            ids.add(cluster.identifier.toString());
        }
        mappedDocument.put(vector.identifier, ids);
    }

    private ArrayList<ClassificationCluster> getClassificationClusters(Vector vector) {
        if (mappedDocument.containsKey(vector.identifier)) {
            return getClassificationCluster(vector.identifier);
        }

        ClusterSimilarityPair[] array = new ClusterSimilarityPair[modelClusters.size()];

        for (int i = 0; i < array.length; i++) {
            ClassificationCluster c = modelClusters.get(i);
            double sim = c.centroid.getSimilarity(vector, Vector.SIMILARITY_COSINE);

            array[i] = new ClusterSimilarityPair();
            array[i].cluster = c;
            array[i].similarity = sim;
        }

        Arrays.sort(array);

        ArrayList<ClassificationCluster> result = new ArrayList<>();
        for (int i = 0; i < neiborsToConsider; i++) {
            result.add(array[array.length - 1 - i].cluster);
        }

        return result;
    }

    private ArrayList<ClassificationCluster> getClassificationCluster(String identifier) {
        ArrayList<ClassificationCluster> result = new ArrayList<>();
        ArrayList<String> ids = mappedDocument.get(identifier);

        for (ClassificationCluster c : modelClusters) {
            if (ids.contains(c.identifier.toString())) {
                result.add(c);
            }
        }

        return result;
    }

    private void updateProgress(int i) {
        progress = i * 100.0 / documents.size();
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

class DocumentEvaluationResult {
    public double precision = 0;
    public double recall = 0;
    public double f1 = 0;

    public String documentIdentifier = "";
    public ArrayList<String> correctResults = new ArrayList<>();
    public ArrayList<String> predictedResults = new ArrayList<>();
}

class ClusterSimilarityPair implements Comparable<ClusterSimilarityPair> {
    public ClassificationCluster cluster = null;
    public double similarity = 0;

    @Override
    public int compareTo(@NotNull ClusterSimilarityPair o) {
        return new Double(o.similarity).compareTo(similarity);
    }
}