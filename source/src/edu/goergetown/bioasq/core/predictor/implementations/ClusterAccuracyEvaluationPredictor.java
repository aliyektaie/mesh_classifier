package edu.goergetown.bioasq.core.predictor.implementations;

import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassificationClusterMeSH;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;

/**
 * Created by zghasemi on 5/31/2017.
 */
public class ClusterAccuracyEvaluationPredictor implements IMeshPredictor {
    private ArrayList<String> tags = new ArrayList<>();

    public ClusterAccuracyEvaluationPredictor() {
        tags = MeSHUtils.getCheckTagsMeSH(true);
    }

    @Override
    public void initialize(ITaskListener listener) {

    }

    @Override
    public ArrayList<String> predict(DocumentFeatureSet document, ArrayList<String> candidates) {
        ArrayList<String> result = getIntersection(candidates, document.meshList);

        return result;
    }

    @Override
    public IMeshPredictor duplicate() {
        return new ClusterAccuracyEvaluationPredictor();
    }

    @Override
    public String getReportFolderName() {
        return "Clustering Accuracy";
    }

    private String[] getStringList(ArrayList<ClassificationClusterMeSH> meshes) {
        String[] result = new String[meshes.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = meshes.get(i).mesh;
        }

        return result;
    }

    private ArrayList<String> getIntersection(String[] list_1, ArrayList<String> list_2) {
        ArrayList<String> result = new ArrayList<>();

        for (String predicted : list_1) {
            if (list_2.contains(predicted)) {
                result.add(predicted);
            }
        }

        return result;
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
}
