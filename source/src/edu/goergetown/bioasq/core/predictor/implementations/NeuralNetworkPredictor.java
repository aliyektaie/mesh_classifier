package edu.goergetown.bioasq.core.predictor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.core.ml.MachineLearningTrainerUtils;
import edu.goergetown.bioasq.core.ml.implementations.NeuralNetworkTrainer;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassificationClusterMeSH;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

/**
 * Created by Yektaie on 6/4/2017.
 */
public class NeuralNetworkPredictor implements IMeshPredictor {
    private ArrayList<String> tags = null;
    private IMachineLearningTrainer neuralNetworkTrainer = new NeuralNetworkTrainer();

    public NeuralNetworkPredictor() {
        tags = MeSHUtils.getCheckTagsMeSH(true);
    }

    @Override
    public void initialize(ITaskListener listener) {

    }

    @Override
    public ArrayList<String> predict(DocumentFeatureSet document, ArrayList<String> candidates) {
        ArrayList<String> result = new ArrayList<>();

        for (String mesh : candidates) {
            if (hasMesh(document, mesh)) {
                result.add(mesh);
            }
        }

        return result;
    }

    private boolean hasMesh(DocumentFeatureSet document, String mesh) {
        boolean result = false;

        String nnModel = Constants.TEXT_CLASSIFIER_FOLDER + "Neural Network" + Constants.BACK_SLASH + mesh + ".nnet";
        if (FileUtils.exists(nnModel)) {
            double[] features = MachineLearningTrainerUtils.getFeatureVector(mesh, document.toVector());
            if (neuralNetworkTrainer.evaluate(features, nnModel)) {
                result = true;
            }
        }

        return result;
    }

    @Override
    public IMeshPredictor duplicate() {
        return new NeuralNetworkPredictor();
    }

    @Override
    public String getReportFolderName() {
        return "Neural Network (Multilayer Perceptron)";
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
