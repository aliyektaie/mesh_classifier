package edu.goergetown.bioasq.core.ml.helper;

import edu.goergetown.bioasq.core.ml.IClassifier;
import edu.goergetown.bioasq.utils.BinaryBuffer;

import java.util.ArrayList;
import java.util.Hashtable;

public class NaiveBayesClassifier implements IClassifier {
    private static final String POSITIVE_CLASS = "true";
    private static final String NEGATIVE_CLASS = "false";

    private double alpha = 0;
    private ArrayList<String> classes = null;
    private Hashtable<String, Double> priorProbs = null;
    private Hashtable<String, Integer> totalWordCounts = null;
    private Hashtable<String, Hashtable<String, Double>> likelihoodProbs = null;
    private ArrayList<String> trainVocab = new ArrayList<>();

    public NaiveBayesClassifier() {
        this(0);
    }

    public NaiveBayesClassifier(double alpha) {
        this.classes = new ArrayList<>();
        this.classes.add(POSITIVE_CLASS);
        this.classes.add(NEGATIVE_CLASS);

        this.alpha = alpha;
        this.priorProbs = new Hashtable<>();
        this.totalWordCounts = new Hashtable<>();
        this.likelihoodProbs = new Hashtable<>();

        for (String cls : classes) {
            this.priorProbs.put(cls, 0.0);
            this.totalWordCounts.put(cls, 0);
            this.likelihoodProbs.put(cls, new Hashtable<>());
        }
    }

    public void learn(double[][] input, String[] vocabulary, double[][] output) {
        int documentCount = input.length;
        Hashtable<String, Integer> labelCounts = new Hashtable<>();
        Hashtable<String, Hashtable<String, Integer>> wordCounts = new Hashtable<>();

        for (int i = 0; i < vocabulary.length; i++) {
            trainVocab.add(vocabulary[i]);
        }

        for (String cls : this.classes) {
            Hashtable<String, Integer> ht = new Hashtable<>();
            wordCounts.put(cls, ht);
            for (String word : vocabulary) {
                ht.put(word, 0);
            }
            labelCounts.put(cls, 0);
        }

        for (int i = 0; i < input.length; i++) {
            double[] document = input[i];
            String cls = getClass(output[i]);
            int labelCount = labelCounts.get(cls);
            labelCounts.put(cls, labelCount + 1);

            for (int j = 0; j < document.length; j++) {
                double weight = document[j];
                if (weight > 0) {
                    String word = vocabulary[j];
                    int wc = wordCounts.get(cls).get(word) + 1;
                    wordCounts.get(cls).put(word, wc);

                    wc = totalWordCounts.get(cls) + 1;
                    totalWordCounts.put(cls, wc);
                }
            }
        }

        for (String cls : classes) {
            priorProbs.put(cls, labelCounts.get(cls) * 1.0 / documentCount);
        }

        for (int i = 0; i < trainVocab.size(); i++) {
            String wordInVocabulary = trainVocab.get(i);
            for (String cls : classes) {
                this.likelihoodProbs.get(cls).put(wordInVocabulary, (wordCounts.get(cls).get(wordInVocabulary) + alpha) / (totalWordCounts.get(cls) + alpha * trainVocab.size()));
            }
        }
    }

    public boolean predict(double[] document) {
        String result = "";
        double probability = -10000000000.0;

        for (String cls : classes) {
            double p = jointProbability(document, cls);

            if (probability < p && p < 0) {
                probability = p;
                result = cls;
            }
        }

        return POSITIVE_CLASS.equals(result);
    }

    private double jointProbability(double[] document, String cls) {
        if (priorProbs.get(cls) == 0.0) {
            return 0;
        }

        double p_sum = Math.log(priorProbs.get(cls));
        for (int i = 0; i < document.length; i++) {
            if (document[i] != 0) {
                double likelihood = likelihoodProbs.get(cls).containsKey(trainVocab.get(i)) ? likelihoodProbs.get(cls).get(trainVocab.get(i)) : 0;
                if (likelihood == 0) {
                    likelihood = alpha / (totalWordCounts.get(cls) + alpha * trainVocab.size());
                }

                p_sum += Math.log(likelihood);
            }
        }

        return p_sum;
    }

    private String getClass(double[] values) {
        double value = values[0];
        if (value == 1)
            return POSITIVE_CLASS;

        return NEGATIVE_CLASS;
    }

    public byte[] serialize() {
        int length = 0;
        length = 4;
        length += 4;
        for (String cls : classes) {
            length += BinaryBuffer.getSerializedLength(cls);
        }

        length += 4;
        for (String cls : trainVocab) {
            length += BinaryBuffer.getSerializedLength(cls);
        }

        length += 4;
        for (String cls : priorProbs.keySet()) {
            length += BinaryBuffer.getSerializedLength(cls);
            length += 4;
        }

        length += 4;
        for (String cls : totalWordCounts.keySet()) {
            length += BinaryBuffer.getSerializedLength(cls);
            length += 4;
        }

        length += 4;
        for (String cls : likelihoodProbs.keySet()) {
            length += BinaryBuffer.getSerializedLength(cls);

            Hashtable<String, Double> ht = likelihoodProbs.get(cls);
            length += 4;
            for (String word : ht.keySet()) {
                length += BinaryBuffer.getSerializedLength(word);
                length += 4;
            }
        }


        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append((float) this.alpha);

        buffer.append(classes.size());
        for (String cls : classes) {
            buffer.append(cls);
        }

        buffer.append(trainVocab.size());
        for (String word : trainVocab) {
            buffer.append(word);
        }

        buffer.append(priorProbs.size());
        for (String cls : priorProbs.keySet()) {
            buffer.append(cls);
            double value = priorProbs.get(cls);
            buffer.append((float) value);
        }

        buffer.append(totalWordCounts.size());
        for (String cls : totalWordCounts.keySet()) {
            buffer.append(cls);
            int value = totalWordCounts.get(cls);
            buffer.append(value);
        }

        buffer.append(likelihoodProbs.size());
        for (String cls : likelihoodProbs.keySet()) {
            buffer.append(cls);
            Hashtable<String, Double> ht = likelihoodProbs.get(cls);
            buffer.append(ht.size());
            for (String word : ht.keySet()) {
                buffer.append(word);
                double v = ht.get(word);
                buffer.append((float) v);
            }
        }

        return buffer.getBuffer();
    }

    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);

        this.classes = new ArrayList<>();
        this.priorProbs = new Hashtable<>();
        this.totalWordCounts = new Hashtable<>();
        this.likelihoodProbs = new Hashtable<>();

        this.alpha = buffer.readFloat();

        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            classes.add(buffer.readString());
        }

        count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            trainVocab.add(buffer.readString());
        }

        count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            String cls = buffer.readString();
            double value = buffer.readFloat();

            priorProbs.put(cls, value);
        }

        count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            String cls = buffer.readString();
            int value = buffer.readInt();

            totalWordCounts.put(cls, value);
        }

        count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            String cls = buffer.readString();

            Hashtable<String, Double> ht = new Hashtable<>();
            int size = buffer.readInt();

            for (int j = 0; j < size; j++) {
                String word = buffer.readString();
                double value = buffer.readFloat();

                ht.put(word, value);
            }

            likelihoodProbs.put(cls, ht);
        }
    }
}
