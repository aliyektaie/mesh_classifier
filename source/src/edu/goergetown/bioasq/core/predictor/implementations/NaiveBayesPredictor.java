package edu.goergetown.bioasq.core.predictor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.core.ml.MachineLearningTrainerUtils;
import edu.goergetown.bioasq.core.ml.helper.NaiveBayesClassifier;
import edu.goergetown.bioasq.core.ml.implementations.NaiveBayesTrainer;
import edu.goergetown.bioasq.core.model.classification.ClassificationClusterMeSH;
import edu.goergetown.bioasq.core.predictor.IMeshPredictor;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 6/15/2017.
 */
public class NaiveBayesPredictor implements IMeshPredictor {
    private IMachineLearningTrainer naiveBayesTrainer = new NaiveBayesTrainer();
    private static Hashtable<String, NaiveBayesClassifier> classifiersList = new Hashtable<>();
    private Hashtable<String, NaiveBayesClassifier> classifiers = new Hashtable<>();
    private static double loadThreshold = 1;
//    private static double loadThreshold = 0.00005;

    public NaiveBayesPredictor() {
        classifiers.putAll(classifiersList);
    }

    @Override
    public void initialize(ITaskListener listener) {
        String folder = Constants.TEXT_CLASSIFIER_FOLDER + naiveBayesTrainer.getTitle();
        ArrayList<String> files = FileUtils.getFiles(folder);

        for (int i = 0; i < files.size(); i++) {
            if (i % 10 == 0)
                listener.setProgress(i, files.size());

            String mesh = files.get(i);
            mesh = mesh.substring(mesh.lastIndexOf(Constants.BACK_SLASH) + 1);
            mesh = mesh.substring(0, mesh.lastIndexOf("."));

            double prob = MeSHProbabilityDistribution.getProbability(mesh);
            if (prob > loadThreshold) {
                NaiveBayesClassifier classifier = new NaiveBayesClassifier();
                classifier.load(FileUtils.readBinaryFile(files.get(i)));
                classifiersList.put(mesh, classifier);
//                listener.log("    -> loaded: " + mesh);
            }
        }

        listener.log("   -> " + classifiersList.size() + " classifiers loaded");

        classifiers.putAll(classifiersList);
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

        if (classifiers.containsKey(mesh)) {
            NaiveBayesClassifier classifier = classifiers.get(mesh);
            result = classifier.predict(MachineLearningTrainerUtils.getFeatureVector(mesh, document.toVector()));
        } else {
            String nnModel = Constants.TEXT_CLASSIFIER_FOLDER + "Naïve Bayes" + Constants.BACK_SLASH + mesh + ".nb";
            if (FileUtils.exists(nnModel)) {
                double[] features = MachineLearningTrainerUtils.getFeatureVector(mesh, document.toVector());
                if (naiveBayesTrainer.evaluate(features, nnModel)) {
                    result = true;
                }
            }
        }

        return result;
    }


    @Override
    public IMeshPredictor duplicate() {
        return new NaiveBayesPredictor();
    }

    @Override
    public String getReportFolderName() {
        return "Naïve Bayes";
    }

    private String[] getStringList(ArrayList<ClassificationClusterMeSH> meshes) {
        String[] result = new String[meshes.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = meshes.get(i).mesh;
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
