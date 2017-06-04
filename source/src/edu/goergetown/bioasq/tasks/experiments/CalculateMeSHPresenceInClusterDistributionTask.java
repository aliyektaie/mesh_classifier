package edu.goergetown.bioasq.tasks.experiments;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.Guid;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.classification.ClassifierParameter;
import edu.goergetown.bioasq.core.model.classification.MeSHClassificationModelBase;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.tasks.train.MeSHTermInDocumentTextClassifierTrainerTask;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Yektaie on 6/1/2017.
 */
public class CalculateMeSHPresenceInClusterDistributionTask extends BaseTask {
    private SubTaskInfo CALCULATE_TASK = new SubTaskInfo("Calculating probabilities", 100);

    @Override
    public void process(ITaskListener listener) {
        ClassifierParameter parameter = new ClassifierParameter();
        parameter.termExtractionMethod = "bm25f";
        parameter.clusteringMethod = "adaptive";
        parameter.clusteringParameter = "0.17";

        ArrayList<Cluster> clusters = MeSHClassificationModelBase.loadClusters(listener, parameter);
        listener.log("Clusters loaded");

        ArrayList<ISubTaskThread> threads = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            ProbabilityCalculatorThread t = new ProbabilityCalculatorThread();
            for (int j = i; j < clusters.size(); j+=Constants.CORE_COUNT) {
                t.clusters.add(clusters.get(j));
            }

            t.meshes = loadMeshList();
            threads.add(t);
        }

        executeWorkerThreads(listener, CALCULATE_TASK, threads);
        ArrayList<StringIntPair> meshes = loadMeshList();
        for (int i = 0; i < meshes.size(); i++) {
            StringIntPair p = meshes.get(i);

            for (int j = 0; j < Constants.CORE_COUNT; j++) {
                p.count += ((ProbabilityCalculatorThread)threads.get(j)).meshes.get(i).count;
            }
        }

        saveResults(listener, meshes, clusters, parameter);
    }

    private void saveResults(ITaskListener listener, ArrayList<StringIntPair> meshes, ArrayList<Cluster> clusters, ClassifierParameter parameter) {
        StringIntPair[] dist = new StringIntPair[meshes.size()];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = meshes.get(i);
        }

        Arrays.sort(dist);
        StringBuilder result = new StringBuilder();
        result.append("ID,MeSH,Probability");
        for (int i = 0; i < dist.length; i++) {
            result.append(String.format("\r\n%d,%s,%.6f", i, dist[i].name.replace(",", ""), dist[i].count * 1.0 / clusters.size()));
        }

        FileUtils.writeText(Constants.REPORTS_FOLDER + parameter.toString() + Constants.BACK_SLASH + "MeSH Presence Distribution in Clusters.csv", result.toString());
    }

    private ArrayList<StringIntPair> loadMeshList() {
        ArrayList<StringIntPair> result = new ArrayList<>();

        String[] lines = FileUtils.readAllLines(MeSHTermInDocumentTextClassifierTrainerTask.getMeSHListFilePath());
        for (int i = 0; i < lines.length; i++) {
            String mesh = lines[i];

            StringIntPair p = new StringIntPair();
            p.name = mesh.replace("COMMMMA", ",");
            result.add(p);
        }

        return result;
    }


    @Override
    protected String getTaskClass() {
        return CLASS_EXPERIMENT;
    }

    @Override
    protected String getTaskTitle() {
        return "Calculate MeSH Presence in Cluster Distribution";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(CALCULATE_TASK);

        return result;
    }
}

class StringIntPair implements Comparable<StringIntPair> {
    public String name = "";
    public int count = 0;

    @Override
    public int compareTo(StringIntPair o) {
        return new Integer(o.count).compareTo(count);
    }
}

class ProbabilityCalculatorThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0;

    public ArrayList<Cluster> clusters = new ArrayList<>();
    public ArrayList<StringIntPair> meshes = new ArrayList<>();

    @Override
    public void run() {
        for (int i = 0; i < clusters.size(); i++) {
            progress = i * 100.0 / clusters.size();

            Cluster cluster = clusters.get(i);
            for (StringIntPair pair : meshes) {
                if (cluster.centroid.containsFeature(pair.name)) {
                    pair.count++;
                }
            }
        }

        finished = true;
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