package edu.goergetown.bioasq.core.ml;

/**
 * Created by zghasemi on 6/15/2017.
 */
public interface IClassifier {
    void learn(double[][] input, String[] vocabulary, double[][] output);

    boolean predict(double[] document);

    byte[] serialize();

    void load(byte[] data);
}
