package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.model.mesh.MeSHVectorFile;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/23/2017.
 */
public class AdaptiveMeSHListClustererTask extends BaseTask {
    private double ADD_SUBSET_OF_DATA_SET_IN_ITERATION = 0.05;
    // candidates: 0.98, 0.96, 0.89, 0.81, 0.77
    private double CLUSTERING_SIMILARITY_THRESHOLD = 0.98;

    private SubTaskInfo CLUSTER_TASK = new SubTaskInfo("Adaptive Clustering Task", 10);
    private SubTaskInfo LOADING_DOCUMENT = new SubTaskInfo("Loading documents", 1);

    @Override
    public void process(ITaskListener listener) {
        listener.setCurrentState("Loading documents");
        ArrayList<Vector> allVectors = loadDocuments(listener);
        ArrayList<Cluster> clusters = new ArrayList<>();

        boolean clustersChanged = true;
        int iteration = 0;

        while (clustersChanged) {
            iteration++;
            clustersChanged = false;

            ArrayList<Vector> vectors = getVectorsSubset(allVectors, iteration);
            listener.setCurrentState(String.format("Clustering documents, iteration %d [Documents: %d] [Clusters: %d]", iteration, vectors.size(), clusters.size()));
            boolean subClusterChanged = true;

            int subIteration = 0;
            while (subClusterChanged) {
                listener.setCurrentState(String.format("Clustering documents, iteration %d [Documents: %d] [Clusters: %d]", iteration, vectors.size(), clusters.size()));
                subClusterChanged = false;
                subIteration++;
                int updateCount = vectors.size() / 1000;
                for (int i = 0; i < vectors.size(); i++) {
                    if (i % updateCount == 0) {
                        listener.setProgress(i, vectors.size());
                    }

                    Vector vector = vectors.get(i);
                    Cluster cluster = getClusterForVector(clusters, vector);
                    if (cluster == null) {
                        clustersChanged = true;
                        subClusterChanged = true;
                        addNewCluster(clusters, vector);
                        listener.setCurrentState(String.format("Clustering documents, iteration %d [Documents: %d] [Clusters: %d]", iteration, vectors.size(), clusters.size()));
                    } else {
                        cluster.addVector(vector, false);
                    }
                }

                ArrayList<Cluster> emptyClusters = new ArrayList<>();
                int clusterIndex = 0;
                for (Cluster cluster : clusters) {
                    cluster.updateCentroid();
                    if (cluster.vectors.size() == 0) {
                        emptyClusters.add(cluster);
                        subClusterChanged = true;
                        clustersChanged = true;
                    } else {
                        String path = createOutputFolder(iteration, subIteration) + "cluster-" + clusterIndex + ".txt";
                        cluster.save(path);
                        cluster.vectors.clear();
                        clusterIndex++;
                    }
                }

                if (emptyClusters.size() > 0) {
                    clusters.removeAll(emptyClusters);
                }

                listener.setCurrentState(String.format("Processing clusters for iteration report, iteration %d [Documents: %d] [Clusters: %d]", iteration, vectors.size(), clusters.size()));
                printClusterReports(listener, clusters, iteration, subIteration);
            }
        }

    }

    private String createOutputFolder(int iteration, int subIteration) {
        String folder = Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "adaptive" + Constants.BACK_SLASH + CLUSTERING_SIMILARITY_THRESHOLD + Constants.BACK_SLASH + iteration + Constants.BACK_SLASH + subIteration + Constants.BACK_SLASH;
        if (!FileUtils.exists(folder)) {
            FileUtils.createDirectory(folder);
        }
        return folder;
    }


    private void printClusterReports(ITaskListener listener, ArrayList<Cluster> clusters, int iteration, int subIteration) {
        double[] similarities = new double[clusters.size()];
        double similarityAverage = 0;

        Vector averageCluster = calculateAverageCluster(clusters);

        for (int i = 0; i < clusters.size(); i++) {
            if ( i % 20 == 0) {
                listener.setProgress(i, clusters.size());
            }
            double similarity = clusters.get(i).centroid.cosine(averageCluster);
            similarityAverage += similarity;
            similarities[i] = similarity;
        }

        similarityAverage = similarityAverage / similarities.length;
        double similaritySD = 0;
        for (int i = 0; i < similarities.length; i++) {
            double similarity = similarities[i];
            double diff = similarity - similarityAverage;
            similaritySD += (diff * diff);
        }

        similaritySD = Math.sqrt(similaritySD / (similarities.length - 1));

        listener.log("-------------------------------------------");
        listener.log(String.format("iteration: %d -> %d", iteration, subIteration));
        listener.log("-------------------------------------------");
        listener.log(String.format("   Cluster Count: %d", clusters.size()));
        listener.log(String.format("   Cluster Similarity AVG: %.3f", similarityAverage));
        listener.log(String.format("   Cluster Similarity SD: %.3f", similaritySD));

        String path = Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "adaptive" + Constants.BACK_SLASH + CLUSTERING_SIMILARITY_THRESHOLD + Constants.BACK_SLASH + "report.txt";
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

    private Cluster getClusterForVector(ArrayList<Cluster> clusters, Vector vector) {
        Cluster result = null;
        double similarity = 0;

        for (Cluster cluster : clusters) {
            double sim = cluster.centroid.cosine(vector);
            if (sim > CLUSTERING_SIMILARITY_THRESHOLD) {
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

                            result.add(createMeSHVector(document));
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

    private Vector createMeSHVector(Document document) {
        Vector result = new Vector();
        result.identifier = String.format("year: %s; pmid: %s", String.valueOf(document.metadata.get("year")), document.identifier);

        for (String mesh : document.categories) {
            if (!isCheckTagMeSH(mesh)) {
                double probability = MeSHProbabilityDistribution.getProbability(mesh);

                result.addWeight(mesh, probability);
            }
        }

        result.setLength(1.0);
        result.optimize();

        return result;

    }

    private boolean isCheckTagMeSH(String mesh) {
        return MeSHUtils.getCheckTagsMeSH().contains(mesh);
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

        return result;
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        ArrayList<Object> thresholdCandidates = new ArrayList<>();

        thresholdCandidates.add(new AdaptiveClusteringParameter(0.98));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.96));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.89));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.81));
        thresholdCandidates.add(new AdaptiveClusteringParameter(0.77));

        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        result.put("threshold", thresholdCandidates);

        return result;
    }

    @Override
    public void setParameter(String name, Object value) {
        CLUSTERING_SIMILARITY_THRESHOLD = ((AdaptiveClusteringParameter)value).threshold;
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
            return ((AdaptiveClusteringParameter)obj).threshold == threshold;
        }
        return false;
    }
}
