package edu.goergetown.bioasq.core.ml.implementations;

import edu.goergetown.bioasq.core.ml.EvaluationOnTrainingDataResult;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;

/**
 * Created by Yektaie on 6/4/2017.
 */
public class SupportVectorMachineTrainer implements IMachineLearningTrainer {

    @Override
    public String getTitle() {
        return "Support Vector Machine";
    }

    @Override
    public String getFileExtension() {
        return ".svm";
    }

    @Override
    public EvaluationOnTrainingDataResult train(double[][] input, double[][] output, int featureCount, String savePath) {
        return new EvaluationOnTrainingDataResult();
    }

    @Override
    public String toString() {
        return getTitle();
    }
}