package edu.goergetown.bioasq.tasks.evaluation;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassificationClusterMeSH;
import edu.goergetown.bioasq.core.model.classification.ClassifierParameter;
import edu.goergetown.bioasq.core.model.classification.MeSHClassificationModelBase;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.core.predictor.MeshPredictors;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.tasks.train.MeSHTermInDocumentTextClassifierTrainerTask;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
//        predictor = list.get(0).duplicate();
        predictor = list.get(list.size() - 1).duplicate();
    }

    @Override
    public void process(ITaskListener listener) {
        listener.log("Load model MD5 hash");
        String modelHash = MeSHClassificationModelBase.getModelHash(listener, parameter);
        listener.log(getCacheFilePath(modelHash));

        listener.log("Initializing predictor");
        listener.setCurrentState("Initializing predictor");
        predictor.initialize(listener);

        listener.log("Load model clusters");
        ArrayList<ClassificationCluster> model = MeSHClassificationModelBase.load(listener, parameter);

        listener.log("Loading set document features");
        ArrayList<DocumentFeatureSet> documents = DocumentFeatureSet.loadListFromFile(inputSource.getFeatureSetFolder() + parameter.termExtractionMethod + Constants.BACK_SLASH + inputSource.getFeatureSetFileName());


        listener.log("Classifying documents into clusters");
        ArrayList<ISubTaskThread> threads = createWorkerThreads(model, modelHash, documents);
        executeWorkerThreads(listener, EVALUATE_TASK, threads);

        if (!existsCacheFile(modelHash)) {
            listener.log("Saving cluster map cache file");
            listener.setCurrentState("Saving cluster map cache file");
            Hashtable<String, ArrayList<String>> cachedClusterMaps = getClusterMaps(threads);
            saveCacheFile(listener, modelHash, cachedClusterMaps);
        }

        saveReport(threads, parameter, listener);
    }

    private void saveReport(ArrayList<ISubTaskThread> threads, ClassifierParameter parameter, ITaskListener listener) {
        ArrayList<DocumentEvaluationResult> results = mergeEvaluationResults(threads);
        String reportFolder = createReportFolder(parameter);

        saveClassificationReport(results, reportFolder, listener);
        saveMeshAccuracyReport(results, reportFolder, listener);
        saveByDocumentReport(results, reportFolder, listener);
        saveMeSHCandidates(threads, reportFolder);

        listener.saveLogs(reportFolder + "log.txt");
    }

    private void saveMeSHCandidates(ArrayList<ISubTaskThread> threads, String reportFolder) {
        ArrayList<String> seenCandidates = new ArrayList<>();
        for (ISubTaskThread t : threads)
            seenCandidates.addAll(((EvaluationThread)t).seenCandidates);

        seenCandidates = EvaluationThread.removeDuplicates(seenCandidates);
        StringBuilder content = new StringBuilder();
        for (String candidate : seenCandidates)
            content.append(candidate + "\r\n");

        FileUtils.writeText(reportFolder + "MeSH Candidates.txt", content.toString());
    }

    private void saveByDocumentReport(ArrayList<DocumentEvaluationResult> evaluationResults, String reportFolder, ITaskListener listener) {
        listener.log("Creating per document report");
        listener.setCurrentState("Creating per document report");

        StringBuilder content = new StringBuilder();
        DocumentEvaluationResult[] results = new DocumentEvaluationResult[evaluationResults.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = evaluationResults.get(i);
        }

        Arrays.sort(results, new Comparator<DocumentEvaluationResult>() {
            @Override
            public int compare(DocumentEvaluationResult o1, DocumentEvaluationResult o2) {
                return new Double(o2.f1).compareTo(o1.f1);
            }
        });

        for (int i = 0; i < results.length; i++) {
            if (i % 10 == 0)
                listener.setProgress(i, results.length);

            DocumentEvaluationResult result = results[i];
            ArrayList<String> intersection = getIntersection(result.correctResults, result.predictedResults);

            if (i>0) {
                content.append("\r\n--------------------------------------------------------------------\r\n");
            }

            content.append("Document Identifier: " + result.documentIdentifier);
            content.append("\r\n\tSimple Metrics:");
            content.append(String.format("\r\n\t\tP: %.3f", result.precision));
            content.append(String.format("\r\n\t\tR: %.3f", result.recall));
            content.append(String.format("\r\n\t\tF1: %.3f", result.f1));
            content.append("\r\n\tCorrect Predictions [" + intersection.size() + "]:");
            for (String mesh : intersection) {
                content.append(String.format("\r\n\t\t%s", mesh));
            }

            ArrayList<String> missing = new ArrayList<>();
            for (String mesh : result.correctResults) {
                if (!intersection.contains(mesh)) {
                    missing.add(mesh);
                }
            }
            content.append("\r\n\tMissing Predictions [" + missing.size() + "]:");
            for (String mesh : missing) {
                content.append(String.format("\r\n\t\t%s", mesh));
            }

            ArrayList<String> wrong = new ArrayList<>();
            for (String mesh : result.predictedResults) {
                if (!result.correctResults.contains(mesh)) {
                    wrong.add(mesh);
                }
            }
            content.append("\r\n\tWrong Predictions [" + wrong.size() + "]:");
            for (String mesh : wrong) {
                content.append(String.format("\r\n\t\t%s", mesh));
            }

        }

        FileUtils.writeText(reportFolder + "Detailed Report.txt", content.toString());
    }

    private void saveMeshAccuracyReport(ArrayList<DocumentEvaluationResult> results, String reportFolder, ITaskListener listener) {
        listener.log("Creating MeSH accuracy report");
        listener.setCurrentState("Creating MeSH accuracy report");
        ArrayList<String> unseenMeshes = new ArrayList<>();

        ArrayList<String> meshes = loadMeshList();
        Hashtable<String, MeSHEvaluationResult> evaluationResult = new Hashtable<>();
        for (String mesh : meshes) {
            MeSHEvaluationResult o = new MeSHEvaluationResult();
            o.mesh = mesh;
            evaluationResult.put(mesh, o);
        }

        for (int i = 0; i < results.size(); i++) {
            if (i % 10 == 0)
                listener.setProgress(i, results.size());

            DocumentEvaluationResult result = results.get(i);
            ArrayList<String> intersection = getIntersection(result.correctResults, result.predictedResults);
            for (String mesh : intersection) {
                evaluationResult.get(mesh).correctPrediction++;
            }

            for (String mesh : result.correctResults) {
                MeSHEvaluationResult o = evaluationResult.get(mesh);
                if (o ==  null) {
                    if (!unseenMeshes.contains(mesh)) {
                        unseenMeshes.add(mesh);
                    }
                } else {
                    o.documentCount++;
                }
            }

            for (String mesh : result.predictedResults) {
                if (!intersection.contains(mesh)) {
                    MeSHEvaluationResult o = evaluationResult.get(mesh);
                    if (o == null) {
                        if (!unseenMeshes.contains(mesh)) {
                            unseenMeshes.add(mesh);
                        }
                    } else if (result.correctResults.contains(mesh)) {
                        o.missingPrediction++;
                    } else {
                        o.wrongPrediction++;
                    }
                }
            }
        }

        MeSHEvaluationResult[] finalResult = new MeSHEvaluationResult[evaluationResult.size()];
        for (int i = 0; i < finalResult.length; i++) {
            finalResult[i] = evaluationResult.get(meshes.get(i));
        }

        Arrays.sort(finalResult);

        StringBuilder content = new StringBuilder();

        content.append("MeSH,Document Count,Correct Prediction,Wrong Prediction,Missing Prediction,Correct Percentage,Wrong Percentage,Impact ((%Wrong + %Missing) * Document Count)");
        for (int i = 0; i < finalResult.length; i++) {
            MeSHEvaluationResult r = finalResult[i];
            content.append(String.format("\r\n%s,%d,%d,%d,%d,%.1f,%.1f,%.3f", r.mesh.replace(",", ";;"), r.documentCount, r.correctPrediction, r.wrongPrediction, r.missingPrediction, r.getCorrectPercentage(), r.getWrongPercentage(), r.getImpact()));
        }

        FileUtils.writeText(reportFolder + "MeSH Classification Accuracy.csv", content.toString());

        content = new StringBuilder();
        for (String mesh : unseenMeshes) {
            content.append(mesh);
            content.append("\r\n");
        }

        FileUtils.writeText(reportFolder + "Unseen MeSH List.txt", content.toString());
    }

    private ArrayList<String> getIntersection(ArrayList<String> list_1, ArrayList<String> list_2) {
        ArrayList<String> result = new ArrayList<>();

        for (String predicted : list_1) {
            if (list_2.contains(predicted)) {
                result.add(predicted);
            }
        }

        return result;
    }

    private ArrayList<String> loadMeshList() {
        ArrayList<String> result = new ArrayList<>();

        String[] lines = FileUtils.readAllLines(MeSHTermInDocumentTextClassifierTrainerTask.getMeSHListFilePath());
        for (int i = 0; i < lines.length; i++) {
            String mesh = lines[i];

            mesh = mesh.replace("COMMMMA", ",");
            result.add(mesh);
        }

        return result;
    }

    @NotNull
    private String createReportFolder(ClassifierParameter parameter) {
        String reportFolder = Constants.REPORTS_FOLDER + parameter.toString() + Constants.BACK_SLASH + predictor.getReportFolderName() + Constants.BACK_SLASH;
        if (!FileUtils.exists(reportFolder)) {
            FileUtils.createDirectory(reportFolder);
        }
        return reportFolder;
    }

    @NotNull
    private ArrayList<DocumentEvaluationResult> mergeEvaluationResults(ArrayList<ISubTaskThread> threads) {
        ArrayList<DocumentEvaluationResult> results = new ArrayList<>();
        for (int i = 0; i < threads.size(); i++) {
            EvaluationThread t = (EvaluationThread) threads.get(i);
            results.addAll(t.results);
        }
        return results;
    }

    private void saveClassificationReport(ArrayList<DocumentEvaluationResult> results, String folder, ITaskListener listener) {
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
        String folder = createReportFolder(parameter);

        String path = getCacheFilePath(modelHash);
        return FileUtils.exists(path);
    }

    private String getCacheFilePath(String modelHash) {
        String folder = Constants.REPORTS_FOLDER + parameter.toString() + Constants.BACK_SLASH;
        StringBuilder fileName = new StringBuilder();
        for (int i = 0; i < modelHash.length(); i++) {
            char c = modelHash.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                fileName.append(c);
            }
        }
        return folder + "Document Classification Cache [" + fileName + "].txt";
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
    public ArrayList<String> seenCandidates = new ArrayList<>();

    private int documentCount = 100;

    @Override
    public void run() {
        documentCount = 20; // documents.size();
        for (int i = 0; i < documentCount; i++) {
            updateProgress(i);

            DocumentFeatureSet document = documents.get(i);
            Vector vector = document.toVector();

            ArrayList<ClassificationCluster> clusters = getClassificationClusters(vector);
            cacheClusterMap(vector, clusters);

            ArrayList<String> candidates = new ArrayList<>();
            for (ClassificationCluster cluster : clusters) {
                for (ClassificationClusterMeSH m : cluster.meshes) {
                    candidates.add(m.mesh);
                }
            }

            candidates = removeDuplicates(candidates);
            seenCandidates.addAll(candidates);
            ArrayList<String> predictions = predictor.predict(document, candidates);

            predictions = removeDuplicates(predictions);

            ArrayList<String> correctMesh = document.meshList;
            ArrayList<String> correctPredictions = getCorrectPredictions(predictions, correctMesh);

            DocumentEvaluationResult result = new DocumentEvaluationResult();

            if (predictions.size() != 0) {
                result.precision = correctPredictions.size() * 1.0 / predictions.size();
                result.recall = correctPredictions.size() * 1.0 / correctMesh.size();
                if (result.precision > 0 && result.recall > 0) {
                    result.f1 = 2 * (result.precision * result.recall) / (result.precision + result.recall);
                }
            }

            result.correctResults = correctMesh;
            result.predictedResults = predictions;
            result.documentIdentifier = vector.identifier;

            results.add(result);
        }

        seenCandidates = removeDuplicates(seenCandidates);

        finished = true;
    }

    public static ArrayList<String> removeDuplicates(ArrayList<String> predictions) {
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
        progress = i * 100.0 / documentCount;
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

class MeSHEvaluationResult implements Comparable<MeSHEvaluationResult>{
    public String mesh = "";
    public int documentCount = 0;
    public int correctPrediction = 0;
    public int wrongPrediction = 0;
    public int missingPrediction = 0;

    public double getCorrectPercentage() {
        if (documentCount == 0)
            return 0;

        return correctPrediction * 100.0 / documentCount;
    }

    public double getWrongPercentage() {
        if (documentCount == 0)
            return 0;

        return wrongPrediction * 100.0 / documentCount;
    }

    public double getMissingPercentage() {
        if (documentCount == 0)
            return 0;

        return missingPrediction * 100.0 / documentCount;
    }

    public double getImpact() {
        double a = (getWrongPercentage() + getMissingPercentage()) * documentCount;
        return a;
    }

    @Override
    public int compareTo(@NotNull MeSHEvaluationResult o) {
        return new Double(o.getImpact()).compareTo(getImpact());
    }
}