package edu.goergetown.bioasq.core.ml.implementations;

import edu.goergetown.bioasq.core.ml.EvaluationOnTrainingDataResult;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.core.ml.helper.NaiveBayesClassifier;
import edu.goergetown.bioasq.utils.FileUtils;

/**
 * Created by zghasemi on 6/14/2017.
 */
public class NaiveBayesTrainer implements IMachineLearningTrainer {
    @Override
    public String getTitle() {
        return "Naïve Bayes";
    }

    @Override
    public String getFileExtension() {
        return ".nb";
    }

    @Override
    public EvaluationOnTrainingDataResult train(double[][] input, String[] vocabulary, double[][] output, int featureCount, String savePath) {
        NaiveBayesClassifier classifier = new NaiveBayesClassifier(0.15);
        classifier.learn(input, vocabulary, output);

        FileUtils.writeBinary(savePath, classifier.serialize());
//        classifier = new NaiveBayesClassifier();
//        classifier.load(FileUtils.readBinaryFile(savePath));

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
        NaiveBayesClassifier classifier = new NaiveBayesClassifier();
        classifier.load(FileUtils.readBinaryFile(modelPath));

        return classifier.predict(features);
    }

    @Override
    public String toString() {
        return getTitle();
    }

}

