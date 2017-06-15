package edu.goergetown.bioasq.core.ml.helper;

import edu.goergetown.bioasq.core.ml.IClassifier;
import edu.goergetown.bioasq.utils.BinaryBuffer;

public class VectorSimilarityClassifier implements IClassifier {
    public double[] vector = null;
    public double similarityThreshold = 0;


    @Override
    public void learn(double[][] input, String[] vocabulary, double[][] output) {
        double[] vector = new double[vocabulary.length];
        for (int i = 0; i < input.length; i++) {
            if (output[i][0] == 1) {
                addVector(vector, input[i]);
            }
        }

        this.vector = vector;
        similarityThreshold = 0;
        for (int i = 0; i < input.length; i++) {
            if (output[i][0] == 1) {
                double sim = getSimilarity(vector, input[i]);
                if (sim > similarityThreshold)
                    similarityThreshold = sim;
            }
        }
    }

    private double getSimilarity(double[] vector, double[] values) {
        double result = 0;
        double l1 = 0;
        double l2 = 0;
        double dot = 0;

        for (int i = 0; i < Math.min(vector.length, values.length); i++) {
            l1 += vector[i] * vector[i];
            l2 += values[i] * values[i];
            dot += vector[i] * values[i];
        }

        result = dot / (l1 * l2);

        return result;
    }

    private void addVector(double[] vector, double[] values) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] + values[i];
        }
    }

    @Override
    public boolean predict(double[] document) {
        return getSimilarity(vector, document) >= similarityThreshold;
    }

    @Override
    public byte[] serialize() {
        int length = (2 + vector.length)*4;
        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append((float) similarityThreshold);
        buffer.append(vector.length);
        for (int i = 0; i < vector.length; i++) {
            buffer.append((float) vector[i]);
        }

        return buffer.getBuffer();
    }

    @Override
    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);

        similarityThreshold = buffer.readFloat();
        int size = buffer.readInt();

        vector = new double[size];
        for (int i = 0; i < size; i++) {
            vector[i] = buffer.readFloat();
        }
    }
}
