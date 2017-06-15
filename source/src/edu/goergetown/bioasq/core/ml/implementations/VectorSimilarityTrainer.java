package edu.goergetown.bioasq.core.ml.implementations;

import edu.goergetown.bioasq.core.ml.EvaluationOnTrainingDataResult;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.core.ml.helper.VectorSimilarityClassifier;
import edu.goergetown.bioasq.utils.FileUtils;

/**
 * Created by zghasemi on 6/15/2017.
 */
public class VectorSimilarityTrainer implements IMachineLearningTrainer {
    @Override
    public String getTitle() {
        return "Vector Similarity";
    }

    @Override
    public String getFileExtension() {
        return ".vs";
    }

    @Override
    public EvaluationOnTrainingDataResult train(double[][] input, String[] vocabulary, double[][] output, int featureCount, String savePath) {
        VectorSimilarityClassifier classifier = new VectorSimilarityClassifier();
        classifier.learn(input, vocabulary, output);

        FileUtils.writeBinary(savePath, classifier.serialize());

        int correct = 0;
        int wrong = 0;

        for (int ii = 0; ii < input.length; ii++) {
            boolean predicted = classifier.predict(input[ii]);
            boolean expected = output[ii][0] == 1;

            if (predicted == expected)
                correct++;
            else
                wrong++;
        }

        EvaluationOnTrainingDataResult result = new EvaluationOnTrainingDataResult();
        result.correct = correct;
        result.wrong = wrong;

        return result;
    }

    @Override
    public boolean evaluate(double[] features, String modelPath) {
        VectorSimilarityClassifier classifier = new VectorSimilarityClassifier();
        classifier.load(FileUtils.readBinaryFile(modelPath));

        return classifier.predict(features);
    }

    @Override
    public String toString() {
        return getTitle();
    }
}

