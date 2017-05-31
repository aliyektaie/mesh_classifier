package edu.goergetown.bioasq.tasks.evaluation;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassifierParameter;
import edu.goergetown.bioasq.core.model.classification.MeSHClassificationModelBase;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

/**
 * Created by Yektaie on 5/30/2017.
 */
public class EvaluationTask extends BaseTask {
    private SubTaskInfo EVALUATE_TASK = new SubTaskInfo("Evaluating data set", 10);
    private ClassifierParameter parameter = null;
    private EvaluationInputSource inputSource = EvaluationInputSource.DEV;
    private ArrayList<ClassifierParameter> configurations = new ArrayList<>();

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
    }

    @Override
    public void process(ITaskListener listener) {
        listener.log("Load model clusters");
        ArrayList<ClassificationCluster> model = MeSHClassificationModelBase.load(listener, parameter);
        listener.log("Load model MD5 hash");
        String modelHash = MeSHClassificationModelBase.getModelHash(listener, parameter);

        listener.log("Loading set document features");
        ArrayList<DocumentFeatureSet> documents = DocumentFeatureSet.loadListFromFile(inputSource.getFeatureSetFolder() + parameter.termExtractionMethod + Constants.BACK_SLASH + inputSource.getFeatureSetFileName());

        listener.log("Classifying documents into clusters");
        ArrayList<ISubTaskThread> threads = createWorkerThreads(model, modelHash, documents);
        executeWorkerThreads(listener, EVALUATE_TASK, threads);
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

            result.add(t);
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

class EvaluationInputSource {
    public static final int TYPE_DEV = 1;
    public static final int TYPE_TEST = 2;

    public int type = 0;

    private EvaluationInputSource(int type) {
        this.type = type;
    }

    public static EvaluationInputSource TEST = new EvaluationInputSource(TYPE_TEST);
    public static EvaluationInputSource DEV = new EvaluationInputSource(TYPE_DEV);

    @Override
    public String toString() {
        return type == TYPE_DEV ? "Development" : "Test";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EvaluationInputSource) {
            return type == ((EvaluationInputSource)obj).type;
        }
        return false;
    }

    public String getFeatureSetFolder() {
        if (type == TYPE_DEV)
            return Constants.DEV_DOCUMENT_FEATURES_DATA_FOLDER;

        return Constants.TEST_DOCUMENT_FEATURES_DATA_FOLDER;
    }

    public String getFeatureSetFileName() {
        if (type == TYPE_DEV)
            return "dev_set.bin";

        return "test_set.bin";
    }
}

class EvaluationThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0;
    public ArrayList<ClassificationCluster> modelClusters = null;
    public String modelHash = "";
    public ArrayList<DocumentFeatureSet> documents= null;
    public Hashtable<String, String> mappedDocument = new Hashtable<>();
    public ArrayList<DocumentEvaluationResult> results = new ArrayList<>();
    public IMeshPredictor predictor = null;

    @Override
    public void run() {
        for (int i = 0; i < documents.size(); i++) {
            updateProgress(i);

            DocumentFeatureSet document = documents.get(i);
            Vector vector = document.toVector();

            ClassificationCluster cluster = getClassificationCluster(vector);

            if (cluster != null) {
                cacheClusterMap(vector, cluster);
                ArrayList<String> predictedMesh = predictor.predict(vector, cluster);
                ArrayList<String> correctMesh = document.meshList;
                ArrayList<String> correctPredictions = getCorrectPredictions(predictedMesh, correctMesh);

                DocumentEvaluationResult result = new DocumentEvaluationResult();

                result.precision = correctPredictions.size() * 1.0 / predictedMesh.size();
                result.recall = correctPredictions.size() * 1.0 / correctMesh.size();
                result.f1 = 2 * (result.precision * result.recall) / (result.precision + result.recall);

                result.correctResults = correctMesh;
                result.predictedResults = predictedMesh;
                result.documentIdentifier = vector.identifier;

                results.add(result);
            }
        }

        finished = true;
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

    private void cacheClusterMap(Vector vector, ClassificationCluster cluster) {
        mappedDocument.put(vector.identifier, cluster.identifier.toString());
    }

    private ClassificationCluster getClassificationCluster(Vector vector) {
        ClassificationCluster cluster = null;
        double similarity = -1;

        for (ClassificationCluster c : modelClusters) {
            double sim = c.centroid.getSimilarity(vector, Vector.SIMILARITY_COSINE);
            if (sim > similarity) {
                similarity = sim;
                cluster = c;
            }
        }
        return cluster;
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