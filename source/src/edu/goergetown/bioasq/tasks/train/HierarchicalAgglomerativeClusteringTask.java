package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.mesh.MeSHGraph;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.model.mesh.MeSHVectorFile;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.ClusteringUtils;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class HierarchicalAgglomerativeClusteringTask extends BaseTask {
    private SubTaskInfo LOADING_DOCUMENT = new SubTaskInfo("Loading documents", 1);
    private SubTaskInfo CREATE_DOCUMENT_GRAPH = new SubTaskInfo("Create document graph", 100);
    private static String GRAPH_EDGE_WEIGHT_FILE_PATH = Constants.TEMP_FOLDER + "mesh-mesh-graph-weights.bin";
    private static String TEMP_GRAPH_EDGE_WEIGHT_FILE_PATH = Constants.TEMP_FOLDER + "mesh-mesh-graph-weights.temp.bin";
    private SimilarityType similarityType = SimilarityType.cosine();

    @Override
    public void process(ITaskListener listener) {
        createGraphSortedWeightFile(listener);


    }

    private void createGraphSortedWeightFile(ITaskListener listener) {
        if (FileUtils.exists(GRAPH_EDGE_WEIGHT_FILE_PATH)) {
            return;
        }

        ArrayList<Vector> vectors = loadDocuments(listener);
        MeSHGraph graph = new MeSHGraph(similarityType.value);

        if (!FileUtils.exists(TEMP_GRAPH_EDGE_WEIGHT_FILE_PATH)) {
            graph.createUnsortedWeightFile(vectors, this, CREATE_DOCUMENT_GRAPH, listener, TEMP_GRAPH_EDGE_WEIGHT_FILE_PATH);
        }

        graph.createSortedWeightFile(TEMP_GRAPH_EDGE_WEIGHT_FILE_PATH, GRAPH_EDGE_WEIGHT_FILE_PATH);
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
                    if (FileUtils.exists(pathToDocuments)) {
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

    private boolean isCheckTagMeSH(String mesh) {
        return MeSHUtils.getCheckTagsMeSH().contains(mesh);
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "MeSH Graph-Based Clustering";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(LOADING_DOCUMENT);
        result.add(CREATE_DOCUMENT_GRAPH);

        return result;
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        ArrayList<Object> thresholdCandidates = new ArrayList<>();

        thresholdCandidates.add(SimilarityType.cosine());
        thresholdCandidates.add(SimilarityType.jaccard());

        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        result.put("similarityType", thresholdCandidates);

        return result;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            this.similarityType = (SimilarityType) value;
        }
    }

    @Override
    public Object getParameter(String name) {
        return similarityType;
    }

}

class SimilarityType {
    public String name = "";
    public int value = 0;

    public SimilarityType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static SimilarityType jaccard() {
        return new SimilarityType("Jaccard", Vector.SIMILARITY_JACCARD);
    }

    public static SimilarityType cosine() {
        return new SimilarityType("Cosine", Vector.SIMILARITY_COSINE);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimilarityType) {
            return value == ((SimilarityType)obj).value;
        }
        return false;
    }
}