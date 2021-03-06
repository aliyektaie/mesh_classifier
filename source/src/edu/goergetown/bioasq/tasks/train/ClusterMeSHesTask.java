package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.*;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.model.*;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.implementation.KMeansClusterer;
import edu.goergetown.bioasq.core.task.*;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.ClusteringUtils;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.*;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class ClusterMeSHesTask extends BaseTask {
    private SubTaskInfo LOADING_DATA = new SubTaskInfo("Loading MeSH from dataset", 1);
    private SubTaskInfo CLUSTER_MESH = new SubTaskInfo("Clustering MeSH [%s] [iteration: %d]", 10);
    private IMeSHClusterer clusterer = new KMeansClusterer(2000);
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
            saveClusterInIteration(clusters, createOutputFolder(i), i == iteration - 1, listener, i);
        }

    }

    private ArrayList<Cluster> loadPreviouslySavedClusters(IMeSHClusterer clusterer, int iteration) {
        String folder = clusterer.getSavingDestinationPath() + iteration + Constants.BACK_SLASH;

        ArrayList<Cluster> result = Cluster.loadClusters(folder + "clusters.bin");
        for (int i = 0; i < result.size(); i++) {
            Cluster cluster = result.get(i);
            cluster.vectors.clear();
        }

        return result;
    }

    private int whichIterationToStartFrom(IMeSHClusterer clusterer) {
        int result = -1;

        for (int i = 0; i < clusterer.numberOfIteration(); i++) {
            String folder = clusterer.getSavingDestinationPath() + i + Constants.BACK_SLASH;
            if (!FileUtils.exists(folder)) {
                break;
            } else {
                result = i;
            }
        }

        result++;

        return result;
    }

    private void printClusterReports(ITaskListener listener, ArrayList<Cluster> clusters, int iteration, int subIteration, int removedCount) {
        double[] similarities = new double[clusters.size()];
        int[] distribution = new int[clusters.size()];
        double similarityAverage = 0;
        double distributionAverage = 0;

        Vector averageCluster = calculateAverageCluster(clusters);

        for (int i = 0; i < clusters.size(); i++) {
            if (i % 20 == 0) {
                listener.setProgress(i, clusters.size());
            }
            double similarity = clusters.get(i).centroid.getSimilarity(averageCluster, Vector.SIMILARITY_COSINE);
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

        String path = Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "k-means" + Constants.BACK_SLASH + clusterer.numberOfCluster() + Constants.BACK_SLASH + "report-" + iteration + ".txt";
        listener.saveLogs(path);
    }

    private Vector calculateAverageCluster(ArrayList<Cluster> clusters) {
        Vector result = new Vector();

        for (Cluster cluster : clusters) {
            result = result.add(cluster.centroid);
            result.optimize();
        }

        return result;
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
        String folder = clusterer.getSavingDestinationPath() + i + Constants.BACK_SLASH;
        if (!FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }
        return folder;
    }

    private int[] saveClusterInIteration(ArrayList<Cluster> clusters, String folder, boolean last, ITaskListener listener, int iteration) {
        int[] documentCounts = new int[clusters.size()];

        int clusterIndex = 0;
        ArrayList<Cluster> toRemove = new ArrayList<>();

        for (Cluster cluster : clusters) {
            cluster.updateCentroid();

            documentCounts[clusterIndex] = cluster.vectors.size();
            clusterIndex++;

            if (cluster.vectors.size() < 10) {
//            if (cluster.vectors.size() < 10 || cluster.vectors.size() > 2500) {
                toRemove.add(cluster);
            }

        }

        clusters.removeAll(toRemove);
        Cluster.saveClusters(folder + "clusters.bin", clusters);

        printClusterReports(listener, clusters, iteration, 0, toRemove.size());
        for (Cluster cluster : clusters) {
            cluster.clearVectors();
        }

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

                            result.add(ClusteringUtils.createMeSHVector(document));
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

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        ArrayList<Object> clusterers = new ArrayList<>();

        clusterers.add(new KMeansClusterer(2000));
        clusterers.add(new KMeansClusterer(3000));
        clusterers.add(new KMeansClusterer(4000));
        clusterers.add(new KMeansClusterer(5000));
        clusterers.add(new KMeansClusterer(6000));
        clusterers.add(new KMeansClusterer(7000));
        clusterers.add(new KMeansClusterer(8000));

        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        result.put("k-means", clusterers);

        return result;
    }

    @Override
    public void setParameter(String name, Object value) {
        clusterer = (IMeSHClusterer) value;
    }

    @Override
    public Object getParameter(String name) {
        return clusterer;
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