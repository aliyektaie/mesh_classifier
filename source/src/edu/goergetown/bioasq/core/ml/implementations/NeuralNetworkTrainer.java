package edu.goergetown.bioasq.core.ml.implementations;

import edu.goergetown.bioasq.core.ml.EvaluationOnTrainingDataResult;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

/**
 * Created by Yektaie on 6/4/2017.
 */
public class NeuralNetworkTrainer implements IMachineLearningTrainer {

    @Override
    public String getTitle() {
        return "Neural Network";
    }

    @Override
    public String getFileExtension() {
        return ".nnet";
    }

    @Override
    public EvaluationOnTrainingDataResult train(double[][] inputs, String[] vocabulary, double[][] outputs, int featureCount, String savePath) {
        MultiLayerPerceptron neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, featureCount, 20, 1);

        DataSet trainingSet = new DataSet(featureCount, 1);
        for (int ii = 0; ii < inputs.length; ii++) {
            trainingSet.addRow(new DataSetRow(inputs[ii], outputs[ii]));
        }
        neuralNetwork.learn(trainingSet);
        neuralNetwork.save(savePath);

        int correct = 0;
        int wrong = 0;

        for (int ii = 0; ii < inputs.length; ii++) {
            neuralNetwork.setInput(inputs[ii]);
            neuralNetwork.calculate();
            double[] result = neuralNetwork.getOutput();
            int value = result[0] > 0.5 ? 1 : 0;
            int expected = (int) outputs[ii][0];

            if (value == expected)
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
    public boolean evaluate(double[] features, String filePath) {
        NeuralNetwork network = NeuralNetwork.createFromFile(filePath);
        network.setInput(features);
        double[] result = network.getOutput();
        return result[0] > 0.5;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
