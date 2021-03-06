package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.model.mesh.MeSHVectorFile;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.ClusteringUtils;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/23/2017.
 */
public class AdaptiveMeSHListClustererTask extends BaseTask {
    private final int SIMILARITY_TYPE = Vector.SIMILARITY_COSINE;
    private double ADD_SUBSET_OF_DATA_SET_IN_ITERATION = 0.01;
    private double CLUSTERING_SIMILARITY_THRESHOLD = 0.03;

    private SubTaskInfo LOADING_DOCUMENT = new SubTaskInfo("Loading documents", 1);
    private SubTaskInfo CLUSTER_TASK = new SubTaskInfo("Adaptive Clustering Task", 100);
    private SubTaskInfo MERGE_CLUSTER_TASK = new SubTaskInfo("Updating cluster centroids", 1);

    @Override
    public void process(ITaskListener listener) {
        AdaptiveClustererState state = initializeClusteringState(listener);
        LOADING_DOCUMENT.estimation = 0;

        while (state.clustersChanged) {
            int threadCount = state.iteration == 0 ? 1 : Constants.CORE_COUNT;

            state.iteration++;
            state.clustersChanged = false;

            showProgress(listener, state);

            ArrayList<ISubTaskThread> workerThreads = createWorkerThreads(state, threadCount, listener);
            CLUSTER_TASK.title = "Clustering Iteration: " + state.iteration;
            executeWorkerThreads(listener, CLUSTER_TASK, workerThreads);
            listener.log("Clustering threads finished");
            state.clusters = mergeThreadClusters(listener, workerThreads, state.clusters);
            listener.log("Clustering threads results merged");

            ArrayList<Cluster> clustersToBeRemoved = processClustersAfterIteration(listener, state);

            if (clustersToBeRemoved.size() > 0) {
                state.clusters.removeAll(clustersToBeRemoved);
            }

            listener.setCurrentState("Saving clusters");
            listener.log("Saving clusters");
            String path = createOutputFolder(state.iteration) + "clusters.bin";
            Cluster.saveClusters(path, state.clusters);

            listener.log("Creating iteration report");
            listener.setCurrentState(String.format("Processing clusters for iteration report, iteration %d [Documents: %d] [Clusters: %d]", state.iteration, state.vectors.size(), state.clusters.size()));
            printClusterReports(listener, state.clusters, state.iteration, 0, clustersToBeRemoved.size());

            for (Cluster cluster : state.clusters) {
                cluster.vectors.clear();
            }
        }

    }

    @NotNull
    private ArrayList<Cluster> processClustersAfterIteration(ITaskListener listener, AdaptiveClustererState state) {
        ArrayList<ISubTaskThread> threads = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            ClusterProcessorAfterIterationThread t = new ClusterProcessorAfterIterationThread();
            t.clusters = new ArrayList<>();
            for (int j = i; j < state.clusters.size(); j += Constants.CORE_COUNT) {
                t.clusters.add(state.clusters.get(j));
            }

            t.keepThreshold = (int) (state.vectors.size() * 10.0 / state.clusters.size());

            threads.add(t);
        }

        executeWorkerThreads(listener, MERGE_CLUSTER_TASK, threads);
        state.clusters = new ArrayList<>();
        ArrayList<Cluster> clustersToBeRemoved = new ArrayList<>();
        state.clustersChanged = false;

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            ClusterProcessorAfterIterationThread t = (ClusterProcessorAfterIterationThread) threads.get(i);
            state.clusters.addAll(t.clustersToKeep);
            clustersToBeRemoved.addAll(t.clustersToBeRemoved);
            state.clustersChanged = state.clustersChanged || t.clustersChanged;
        }

        return clustersToBeRemoved;
    }

    private ArrayList<Cluster> mergeThreadClusters(ITaskListener listener, ArrayList<ISubTaskThread> workerThreads, ArrayList<Cluster> clusters) {
        ArrayList<Cluster> result = clusters;

        for (int i = 0; i < workerThreads.size(); i++) {
            AdaptiveClusteringThread thread = ((AdaptiveClusteringThread) workerThreads.get(i));
            result.addAll(thread.newClusters);

            try {
                for (Cluster clonedcluster : thread.clusters) {
                    clonedcluster.clonedFrom.vectors.addAll(clonedcluster.vectors);
                }
            } catch (Exception ex) {

            }
        }

        return result;
    }

    private ArrayList<ISubTaskThread> createWorkerThreads(AdaptiveClustererState state, int threadCount, ITaskListener listener) {
        ArrayList<ISubTaskThread> result = new ArrayList<>();
        ArrayList<ArrayList<Vector>> vectors = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            vectors.add(new ArrayList<>());
        }

        for (int i = 0; i < state.vectors.size(); i++) {
            vectors.get(i % threadCount).add(state.vectors.get(i));
        }

        for (int i = 0; i < threadCount; i++) {
            AdaptiveClusteringThread t = new AdaptiveClusteringThread();

            t.vectorsList = vectors.get(i);
            t.similarityThreshold = CLUSTERING_SIMILARITY_THRESHOLD;
            t.similarityType = SIMILARITY_TYPE;
            t.state = state;
            t.clusters = cloneClusters(state.clusters);
            t.printClustersCount = threadCount == 1;
            t.listener = listener;

            result.add(t);
        }

        return result;
    }

    private ArrayList<Cluster> cloneClusters(ArrayList<Cluster> clusters) {
        ArrayList<Cluster> result = new ArrayList<>();

        for (Cluster cluster : clusters) {
            Cluster c = new Cluster();
            c.centroid = cluster.centroid;
            c.clonedFrom = cluster;

            result.add(c);
        }

        return result;
    }

    private AdaptiveClustererState initializeClusteringState(ITaskListener listener) {
        AdaptiveClustererState state = new AdaptiveClustererState();

        listener.setCurrentState("Loading documents");
        state.vectors = loadDocuments(listener);

        int count = 0;
        while (FileUtils.exists(createOutputFolder(count + 1, false) + "clusters.bin")) {
            count++;
        }

        if (count == 0) {
            state.clusters = new ArrayList<>();
        } else {
            state.clusters = Cluster.loadClusters(createOutputFolder(count, false) + "clusters.bin");
            for (Cluster c : state.clusters) {
                c.vectors.clear();
            }
        }
        state.clustersChanged = true;
        state.iteration = count;
        return state;
    }

    private void showProgress(ITaskListener listener, AdaptiveClustererState state) {
        listener.setCurrentState(String.format("Clustering documents, iteration %d [Documents: %d] [Clusters: %d]", state.iteration, state.vectors.size(), state.clusters.size()));
    }

    private String createOutputFolder(int iteration) {
        return createOutputFolder(iteration, true);
    }

    private String createOutputFolder(int iteration, boolean create) {
        String folder = Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "adaptive" + Constants.BACK_SLASH + CLUSTERING_SIMILARITY_THRESHOLD + Constants.BACK_SLASH + iteration + Constants.BACK_SLASH;
        if (create && !FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }
        return folder;
    }


    private void printClusterReports(ITaskListener listener, ArrayList<Cluster> clusters, int iteration, int subIteration, int removedCount) {
        double[] similarities = new double[clusters.size()];
        int[] distribution = new int[clusters.size()];
        double similarityAverage = 0;
        double distributionAverage = 0;

        Vector averageCluster = calculateAverageCluster(clusters, listener);

        for (int i = 0; i < clusters.size(); i++) {
            if (i % 20 == 0) {
                listener.setProgress(i, clusters.size());
            }
            double similarity = clusters.get(i).centroid.getSimilarity(averageCluster, SIMILARITY_TYPE);
            similarityAverage += similarity;
            similarities[i] = similarity;

            distribution[i] = clusters.get(i).vectors.size();
            distributionAverage += clusters.get(i).vectors.size();
        }

        similarityAverage = similarityAverage / similarities.length;
        distributionAverage = distributionAverage / distribution.length;
        double similaritySD = 0;
        double distributionSD = 0;
        for (int i = 0; i < similarities.length; i++) {
            double similarity = similarities[i];
            double diff = similarity - similarityAverage;
            similaritySD += (diff * diff);

            double distrib = distribution[i];
            diff = distrib - distributionAverage;
            distributionSD += (diff * diff);
        }

        similaritySD = Math.sqrt(similaritySD / (similarities.length - 1));
        distributionSD = Math.sqrt(distributionSD / (similarities.length - 1));

        listener.log("-------------------------------------------");
        listener.log(String.format("iteration: %d", iteration, subIteration));
        listener.log("-------------------------------------------");
        listener.log(String.format("   Cluster Count: %d", clusters.size()));
        listener.log(String.format("   Cluster Similarity AVG: %.3f", similarityAverage));
        listener.log(String.format("   Cluster Similarity SD: %.3f", similaritySD));
        listener.log(String.format("   Cluster Document Count AVG: %.3f", distributionAverage));
        listener.log(String.format("   Cluster Document Count SD: %.3f", distributionSD));
        listener.log(String.format("   Cluster Removed: %d", removedCount));

        String path = Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "adaptive" + Constants.BACK_SLASH + CLUSTERING_SIMILARITY_THRESHOLD + Constants.BACK_SLASH + "report-" + iteration + ".txt";
        listener.saveLogs(path);
    }

    private Vector calculateAverageCluster(ArrayList<Cluster> clusters, ITaskListener listener) {
        Vector result = new Vector();
        int i = 0;
        listener.log("Calculating average vector");
        listener.setCurrentState("Calculating average vector");

        for (Cluster cluster : clusters) {
            i++;

            if (i % 10 == 0)
                listener.setProgress(i, clusters.size());

            result = result.add(cluster.centroid);
            result.optimize();
        }

        return result;
    }

    private ArrayList<Vector> getVectorsSubset(ArrayList<Vector> allVectors, int iteration) {
        int count = (int) Math.min(allVectors.size(), ADD_SUBSET_OF_DATA_SET_IN_ITERATION * iteration * allVectors.size());
        ArrayList<Vector> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            result.add(allVectors.get(i));
        }

        return result;
    }

    private ArrayList<Vector> loadDocuments(ITaskListener listener) {
        ArrayList<Vector> result = new ArrayList<>();

        if (MeSHVectorFile.exists()) {
            MeSHVectorFile.load(result);
        } else {
            ArrayList<Integer> years = DocumentListUtils.getAvailableDocumentYears();
            final int[] count = {0};
            int total = getTotalDocumentCount(years);

            listener.setCurrentState("Loading documents ...");

            for (int yearIndex = 0; yearIndex < years.size(); yearIndex++) {
                int year = years.get(yearIndex);
                for (int core = 0; core < Constants.CORE_COUNT; core++) {
                    String pathToDocuments = Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + core + ".bin";
                    Document.iterateOnDocumentListFile(pathToDocuments, new IDocumentLoaderCallback() {
                        @Override
                        public void processDocument(Document document, int i, int totalDocumentCount) {
                            count[0]++;
                            if (count[0] % 1000 == 0) {
                                updateProgress(listener, LOADING_DOCUMENT, count[0], total);
                            }

                            result.add(ClusteringUtils.createMeSHVector(document));
                        }
                    });
                }
            }

            MeSHVectorFile.save(result);
        }

        return result;
    }

    private int getTotalDocumentCount(ArrayList<Integer> years) {
        int result = 0;

        for (int yearIndex = 0; yearIndex < years.size(); yearIndex++) {
            int year = years.get(yearIndex);
            for (int core = 0; core < Constants.CORE_COUNT; core++) {
                String path = Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + core + ".bin";
                result += Document.getDocumentCountInFile(path);
            }
        }

        return result;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Adaptive MeSH Clustering";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(LOADING_DOCUMENT);
        result.add(CLUSTER_TASK);
        result.add(MERGE_CLUSTER_TASK);

        return result;
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        ArrayList<Object> thresholdCandidates = new ArrayList<>();

        thresholdCandidates.add(new AdaptiveClusteringParameter(0.13));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.17));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.21));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.24));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.27));

        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        result.put("threshold", thresholdCandidates);

        return result;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            CLUSTERING_SIMILARITY_THRESHOLD = ((AdaptiveClusteringParameter) value).threshold;
        }
    }

    @Override
    public Object getParameter(String name) {
        return new AdaptiveClusteringParameter(CLUSTERING_SIMILARITY_THRESHOLD);
    }
}

class AdaptiveClusteringParameter {
    public double threshold = 0;

    public AdaptiveClusteringParameter(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return String.format("Document Threshold: %.2f", threshold);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AdaptiveClusteringParameter) {
            return ((AdaptiveClusteringParameter) obj).threshold == threshold;
        }
        return false;
    }
}

class AdaptiveClusteringThread extends Thread implements ISubTaskThread {
    private boolean isFinished = false;
    private double progress = 0;
    public ArrayList<Vector> vectorsList = null;
    public int similarityType = 0;
    public double similarityThreshold = 0.0;
    public ArrayList<Cluster> clusters = null;
    public AdaptiveClustererState state = null;
    public ArrayList<Cluster> newClusters = new ArrayList<>();
    public boolean printClustersCount = false;
    public ITaskListener listener = null;

    @Override
    public void run() {
        int count = vectorsList.size();

        if (printClustersCount) {
            listener.log("First iteration, cluster count details:");
        }

        for (int i = 0; i < count; i++) {
            progress = i * 100.0 / count;
            if (i % 1000 == 0 && printClustersCount) {
                listener.log("   -> " + clusters.size());
            }

            Vector vector = vectorsList.get(vectorsList.size() - i - 1);
            Cluster cluster = getClusterForVector(clusters, vector);
            if (cluster == null) {
                state.clustersChanged = true;
                addNewCluster(clusters, vector);
            } else {
                cluster.addVector(vector, false);
            }
        }


        isFinished = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    private Cluster getClusterForVector(ArrayList<Cluster> clusters, Vector vector) {
        Cluster result = null;
        double similarity = 0;

        for (Cluster cluster : clusters) {
            double sim = cluster.centroid.getSimilarity(vector, similarityType);
            if (sim > similarityThreshold) {
                if (sim > similarity) {
                    result = cluster;
                    similarity = sim;
                }
            }
        }

        return result;
    }

    private void addNewCluster(ArrayList<Cluster> clusters, Vector vector) {
        Cluster cluster;
        cluster = new Cluster();
        cluster.centroid = vector;
        cluster.addVector(vector, false);

        clusters.add(cluster);
        newClusters.add(cluster);
    }
}

class ClusterMergerThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0.0;
    public ArrayList<Cluster> list1 = null;
    public ArrayList<Cluster> list2 = null;
    public ArrayList<Cluster> result = new ArrayList<>();
    public double similarityThreshold = 0;
    public int similarityType = 0;

    @Override
    public void run() {
        int i = 0;
        for (Cluster cluster : list1) {
            progress = i * 100.0 / list1.size();
            i++;

            ArrayList<Cluster> similarClusters = getSimilarClusters(cluster, list2);

            Cluster resultCluster = new Cluster();
            resultCluster.vectors.addAll(cluster.vectors);
            for (Cluster similarCluster : similarClusters) {
                resultCluster.vectors.addAll(similarCluster.vectors);
                list2.remove(similarCluster);
            }

            cluster.updateCentroid();
            result.add(cluster);
        }

        result.addAll(list2);

        finished = true;
    }

    private ArrayList<Cluster> getSimilarClusters(Cluster cluster, ArrayList<Cluster> list) {
        ArrayList<Cluster> result = new ArrayList<>();

        for (Cluster c : list) {
            if (c.centroid.getSimilarity(cluster.centroid, similarityType) > similarityThreshold) {
                result.add(c);
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

class AdaptiveClustererState {
    public ArrayList<Vector> vectors = null;
    public ArrayList<Cluster> clusters = null;
    public boolean clustersChanged = false;
    public int iteration = 0;
}

class ClusterProcessorAfterIterationThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0;
    public ArrayList<Cluster> clustersToBeRemoved = new ArrayList<>();
    public ArrayList<Cluster> clustersToKeep = new ArrayList<>();
    public ArrayList<Cluster> clusters = null;
    public boolean clustersChanged = false;
    public int keepThreshold = 0;

    @Override
    public void run() {
        for (int i = 0; i < clusters.size(); i++) {
            progress = i * 100.0 / clusters.size();
            Cluster cluster = clusters.get(i);

            if (cluster.vectors.size() == 0 || cluster.vectors.size() > keepThreshold) {
                clustersToBeRemoved.add(cluster);
                this.clustersChanged = true;
            } else {
                cluster.updateCentroid();
                clustersToKeep.add(cluster);
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