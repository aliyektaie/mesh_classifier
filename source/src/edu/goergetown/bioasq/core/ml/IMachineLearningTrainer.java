package edu.goergetown.bioasq.core.ml;

public interface IMachineLearningTrainer {
    String getTitle();

    String getFileExtension();

    EvaluationOnTrainingDataResult train(double[][] input, double[][] output, int featureCount, String savePath);

    boolean evaluate(double[] features, String modelPath);
}
