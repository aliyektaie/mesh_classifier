package edu.goergetown.bioasq.core.predictor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.core.ml.MachineLearningTrainerUtils;
import edu.goergetown.bioasq.core.ml.implementations.NeuralNetworkTrainer;
import edu.goergetown.bioasq.core.ml.implementations.SupportVectorMachineTrainer;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassificationClusterMeSH;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

/**
 * Created by Yektaie on 6/5/2017.
 */
public class LinearSVMPredictor implements IMeshPredictor {
    private ArrayList<String> tags = null;
//    private IMachineLearningTrainer neuralNetworkTrainer = new NeuralNetworkTrainer();
    private IMachineLearningTrainer svmTrainer = new SupportVectorMachineTrainer();

    public LinearSVMPredictor() {
        tags = MeSHUtils.getCheckTagsMeSH(true);
    }

    @Override
    public ArrayList<String> predict(DocumentFeatureSet document, ClassificationCluster cluster) {
        String[] clusterMeshes = getStringList(cluster.meshes);
        ArrayList<String> result = new ArrayList<>();

        for (String mesh : clusterMeshes) {
            if (hasMesh(document, mesh)) {
                result.add(mesh);
            }
        }

        return result;
    }

    private boolean hasMesh(DocumentFeatureSet document, String mesh) {
        boolean result = false;

//        String nnModel = Constants.TEXT_CLASSIFIER_FOLDER + "Neural Network" + Constants.BACK_SLASH + mesh + ".nnet";
        String svmModel = Constants.TEXT_CLASSIFIER_FOLDER + "Linear SVM" + Constants.BACK_SLASH + mesh + ".svm";

        double[] features = MachineLearningTrainerUtils.getFeatureVector(mesh, document.toVector());
        if (FileUtils.exists(svmModel)) {
            if (svmTrainer.evaluate(features, svmModel)) {
                result = true;
            }
//        } else if (FileUtils.exists(nnModel)) {
//            if (neuralNetworkTrainer.evaluate(features, nnModel)) {
//                result = true;
//            }
        }

        return result;
    }

    @Override
    public IMeshPredictor duplicate() {
        return new LinearSVMPredictor();
    }

    @Override
    public String getReportFolderName() {
        return "Linear SVM";
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
