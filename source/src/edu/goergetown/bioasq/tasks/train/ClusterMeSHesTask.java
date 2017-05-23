package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.*;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.model.*;
import edu.goergetown.bioasq.core.model.implementation.KMeansClusterer;
import edu.goergetown.bioasq.core.task.*;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class ClusterMeSHesTask extends BaseTask {
    private SubTaskInfo LOADING_DATA = new SubTaskInfo("Loading MeSH from dataset", 1);
    private SubTaskInfo CLUSTER_MESH = new SubTaskInfo("Clustering MeSH [%s] [iteration: %d]", 10);
    private IMeSHClusterer clusterer = new KMeansClusterer(2048);
    private ArrayList<SubTaskInfo> subTasks = null;

    @Override
    public void process(ITaskListener listener) {
        subTasks = null;
        getSubTasks();

        int clusterCount = clusterer.numberOfCluster();

        ArrayList<Vector> vectors = loadDocuments(listener, clusterer);
        ArrayList<Cluster> clusters = null;
        int startIteration = whichIterationToStartFrom(clusterer);
        if (startIteration > 0) {
            clusters = loadPreviouslySavedClusters(clusterer, startIteration - 1);
        } else {
            clusters = getInitialClusters(clusterCount, vectors, clusterer);
        }

        clusterer.initializeClusteringIteration(clusters);

        int iteration = clusterer.numberOfIteration();
        int threadNumber = clusterer.getNumberOfThreads();

        ArrayList<ArrayList<Vector>> vectorsSplitList = splitVectorsList(vectors, threadNumber);

        for (int i = startIteration; i < iteration; i++) {
            listener.log("Start " + clusterer.getName() + " Clustering [iteration: " + (i + 1) + "]");
            listener.setCurrentState(clusterer.getName() + " Clustering [iteration: " + (i + 1) + "]");

            ArrayList<ISubTaskThread> threads = createWorkerThreads(clusters, threadNumber, vectorsSplitList);
            executeWorkerThreads(listener, subTasks.get(i + 1), threads);
            saveClusterInIteration(clusters, createOutputFolder(i), i == iteration - 1, listener);
        }

    }

    private ArrayList<Cluster> loadPreviouslySavedClusters(IMeSHClusterer clusterer, int iteration) {
        ArrayList<Cluster> result = new ArrayList<>();
        String folder = clusterer.getSavingDestinationPath() + "temp-" + iteration + Constants.BACK_SLASH;

        for (int i = 0; i < clusterer.numberOfIteration(); i++) {
            Cluster cluster = new Cluster();
            cluster.load(folder + "cluster-" + i + ".txt");
            cluster.vectors.clear();
        }

        return result;
    }

    private int whichIterationToStartFrom(IMeSHClusterer clusterer) {
        int result = -1;

        for (int i = 0; i < clusterer.numberOfIteration(); i++) {
            String folder = clusterer.getSavingDestinationPath() + "temp-" + i + Constants.BACK_SLASH;
            if (!FileUtils.exists(folder)) {
                break;
            } else {
                result = i;
            }
        }

        result++;

        return result;
    }

    private void printReport(ITaskListener listener, int[] distribution, int removed) {
        double avg = 0;
        for (int count : distribution)
            avg += count;

        avg = avg / distribution.length;
        listener.log(String.format("      Average document per cluster: %.2f", avg));

        double sd = 0;
        for (int count : distribution) {
            double t = count - avg;
            sd += t*t;
        }

        sd = Math.sqrt(sd / (distribution.length - 1));
        listener.log(String.format("      Standard deviation: %.2f", sd));
        listener.log(String.format("      Cluster removed: %d", removed));
        listener.log("");
    }

    private ArrayList<ISubTaskThread> createWorkerThreads(ArrayList<Cluster> clusters, int threadNumber, ArrayList<ArrayList<Vector>> vectorsSplitList) {
        ArrayList<ISubTaskThread> threads = new ArrayList<>();
        for (int j = 0; j < threadNumber; j++) {
            MeSHClassifierThread t = new MeSHClassifierThread();
            t.clusters = clusters;
            t.vectors = vectorsSplitList.get(j);
            t.clusterer = clusterer;

            threads.add(t);
        }
        return threads;
    }

    private String createOutputFolder(int i) {
        String folder = clusterer.getSavingDestinationPath() + "temp-" + i + Constants.BACK_SLASH;
        if (!FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }
        return folder;
    }

    private int[] saveClusterInIteration(ArrayList<Cluster> clusters, String folder, boolean last, ITaskListener listener) {
        int[] documentCounts = new int[clusters.size()];

        int clusterIndex = 0;
        ArrayList<Cluster> toRemove = new ArrayList<>();

        for (Cluster cluster : clusters) {
            cluster.updateCentroid();

            String path = folder + "cluster-" + clusterIndex + ".txt";
            cluster.save(path);

            documentCounts[clusterIndex] = cluster.vectors.size();
            clusterIndex++;

            if (cluster.vectors.size() < 10) {
//            if (cluster.vectors.size() < 10 || cluster.vectors.size() > 2500) {
                toRemove.add(cluster);
            }

            cluster.clearVectors();
        }

        clusters.removeAll(toRemove);
        printReport(listener, documentCounts, toRemove.size());

        if (!last) {
            clusterer.initializeClusteringIteration(clusters);
        }

        return documentCounts;
    }

    private ArrayList<ArrayList<Vector>> splitVectorsList(ArrayList<Vector> vectors, int partCount) {
        ArrayList<ArrayList<Vector>> vectorsSplitedList = new ArrayList<>();
        for (int i = 0; i < partCount; i++) {
            vectorsSplitedList.add(new ArrayList<>());
        }

        for (int i = 0; i < vectors.size(); i++) {
            vectorsSplitedList.get(i % partCount).add(vectors.get(i));
        }
        return vectorsSplitedList;
    }


    private ArrayList<Cluster> getInitialClusters(int clusterCount, ArrayList<Vector> vectors, IMeSHClusterer clusterer) {
        ArrayList<Cluster> clusters = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < clusterCount; i++) {
            int index = (random.nextInt() & 0x0FFFFFFF) % vectors.size();
            Cluster cluster = new Cluster();
            cluster.centroid = vectors.get(index);

            clusters.add(cluster);
        }
//        Vector seed = vectors.get(0);
//        VectorDoublePair[] othersSimilarity = getOthersSimilaritySorted(vectors, seed, clusterer);
//
//        Cluster seedCluster = new Cluster();
//        seedCluster.centroid = seed;
//        clusters.add(seedCluster);
//
//        for (int i = 0; i < clusterCount - 1; i++) {
//            Cluster cluster = new Cluster();
//            cluster.centroid = othersSimilarity[i].vector;
//            clusters.add(cluster);
//        }

        return clusters;

    }

    private VectorDoublePair[] getOthersSimilaritySorted(ArrayList<Vector> vectors, Vector seed, IMeSHClusterer clusterer) {
        VectorDoublePair[] result = new VectorDoublePair[vectors.size() - 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = new VectorDoublePair();

            Vector vector = vectors.get(i + 1);
            result[i].similarity = clusterer.getSimilarity(seed, vector);
            result[i].vector = vector;
        }

        Arrays.sort(result, new Comparator<VectorDoublePair>() {
            @Override
            public int compare(VectorDoublePair o1, VectorDoublePair o2) {
                return new Double(o1.similarity).compareTo(o2.similarity);
            }
        });

        return result;
    }

    private ArrayList<Vector> loadDocuments(ITaskListener listener, IMeSHClusterer clusterer) {
        String path = Constants.TEMP_FOLDER + "documents_mesh.bin";
        ArrayList<Vector> result = new ArrayList<>();

        if (FileUtils.exists(path)) {
            loadMeSHVectorFiles(result, path);
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
                                updateProgress(listener, LOADING_DATA, count[0], total);
                            }

                            result.add(createMeSHVector(document));
                        }
                    });
                }
            }

            saveMeSHVectorFiles(result, path);
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

    private Vector createMeSHVector(Document document) {
        Vector result = new Vector();
        result.identifier = String.format("year: %s; pmid: %s", String.valueOf(document.metadata.get("year")), document.identifier);

        double p_min = MeSHProbabilityDistribution.getMinimumProbability();
        double p_max = MeSHProbabilityDistribution.getMaximumProbability();
        double scale_min = 0.01;

        for (String mesh : document.categories) {
            double probability = MeSHProbabilityDistribution.getProbability(mesh);
            probability = scale_min + (1 - scale_min) * ((probability - p_min) / (p_max - p_min));

            result.addWeight(mesh, 1.0 - probability);
        }

        result.optimize();

        return result;

    }

    private void saveMeSHVectorFiles(ArrayList<Vector> result, String path) {
        ArrayList<byte[]> data = new ArrayList<>();
        int length = 0;

        for (Vector vector : result) {
            byte[] a = vector.serialize();

            length += BinaryBuffer.getSerializedLength(a);
            data.add(a);
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        for (byte[] d : data)
            buffer.append(d);

        FileUtils.writeBinary(path, buffer.getBuffer());
    }

    private void loadMeSHVectorFiles(ArrayList<Vector> result, String path) {
        BinaryBuffer buffer = new BinaryBuffer(FileUtils.readBinaryFile(path));

        while (buffer.canRead()) {
            Vector vector = new Vector();
            vector.load(buffer.readByteArray());
            vector.optimize();

            result.add(vector);
        }
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Create MeSH Cluster";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        if (subTasks == null) {
            ArrayList<SubTaskInfo> result = new ArrayList<>();

            result.add(LOADING_DATA);

            int iteration = clusterer.numberOfIteration();
            for (int i = 0; i < iteration; i++) {
                SubTaskInfo info = new SubTaskInfo(String.format(CLUSTER_MESH.title, clusterer.getName(), i + 1), CLUSTER_MESH.estimation);
                result.add(info);
            }

            subTasks = result;
            return result;
        }

        return subTasks;
    }
}

class MeSHClassifierThread extends Thread implements ISubTaskThread {
    private boolean isFinished = false;
    private double progress = 0.0;
    public ArrayList<Cluster> clusters;
    public ArrayList<Vector> vectors;
    public IMeSHClusterer clusterer;

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void run() {
        for (int i = 0; i < vectors.size(); i++) {
            progress = i * 100.0 / vectors.size();

            Vector vector = vectors.get(i);
            clusterer.classifyVector(vector, clusters);
        }

        isFinished = true;
    }

    @Override
    public double getProgress() {
        return progress;
    }
}

class VectorDoublePair {
    public Vector vector = null;
    public double similarity = 0;
}