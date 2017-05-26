package edu.goergetown.bioasq.core.model;

import edu.goergetown.bioasq.utils.BinaryBuffer;

import java.util.*;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class Vector {
    public final static int SIMILARITY_COSINE = 1;
    public final static int SIMILARITY_JACCARD = 2;

    private ArrayList<FeatureValuePair> features = new ArrayList<>();
    public String identifier = "";
    private boolean optimized = false;
    private double length = -1;

    public void addWeight(String name, double weight) {
        FeatureValuePair pair = new FeatureValuePair();

        pair.name = name;
        pair.value = weight;

        features.add(pair);

        optimized = false;
    }

    public double getSimilarity(Vector v, int type) {
        if (!optimized) {
            optimize();
        }

        if (!v.optimized)
            v.optimize();

        double result = 0;

        switch (type) {
            case SIMILARITY_COSINE:
                result = similarityCosine(v);
                break;
            case SIMILARITY_JACCARD:
                result = similarityJaccard(v);
                break;
        }

        return result;
    }

    private double similarityJaccard(Vector v) {
        int a_b = 0;
        int a = 0;
        int b = 0;

        int i1 = 0;
        int i2 = 0;

        while (i1 < features.size() && i2 < v.features.size()) {
            FeatureValuePair f1 = features.get(i1);
            FeatureValuePair f2 = v.features.get(i2);

            int cmp = f1.name.compareTo(f2.name);
            if (cmp == 0) {
                a_b++;
                a++;
                b++;

                i1++;
                i2++;
            } else if (cmp < 0) {
                i1++;

                a++;
            } else {
                i2++;

                b++;
            }
        }

        return (a_b * 1.0)/ (a + b - a_b);
    }

    private double similarityCosine(Vector v) {
        double dotProduct = 0;
        int i1 = 0;
        int i2 = 0;

        while (i1 < features.size() && i2 < v.features.size()) {
            String f1 = features.get(i1).name;
            String f2 = v.features.get(i2).name;

            int cmp = f1.compareTo(f2);
            if (cmp == 0) {
                dotProduct += (features.get(i1).value * v.features.get(i2).value);

                i1++;
                i2++;
            } else if (cmp < 0) {
                i1++;
            } else {
                i2++;
            }
        }

        return dotProduct / (length * v.length);
    }

    public void optimize() {
        FeatureValuePair[] features = new FeatureValuePair[this.features.size()];
        double l = 0;

        for (int i = 0; i < features.length; i++) {
            features[i] = this.features.get(i);
            l = l + (features[i].value * features[i].value);
        }

        Arrays.sort(features, new Comparator<FeatureValuePair>() {
            @Override
            public int compare(FeatureValuePair f1, FeatureValuePair f2) {
                return f1.name.compareTo(f2.name);
            }
        });

        this.features.clear();
        this.features.addAll(Arrays.asList(features));
        this.length = Math.sqrt(l);

        optimized = true;
    }

    public Vector add(Vector v) {
        Vector result = new Vector();

        if (!optimized) {
            optimize();
        }

        if (!v.optimized)
            v.optimize();

        int i1 = 0;
        int i2 = 0;

        while (i1 < features.size() && i2 < v.features.size()) {
            String f1 = features.get(i1).name;
            String f2 = v.features.get(i2).name;

            int cmp = f1.compareTo(f2);
            if (cmp == 0) {
                result.addWeight(f1, features.get(i1).value + v.features.get(i2).value);

                i1++;
                i2++;
            } else if (cmp < 0) {
                result.addWeight(f1, features.get(i1).value);
                i1++;
            } else {
                result.addWeight(f2, v.features.get(i2).value);
                i2++;
            }
        }

        while (i1 < features.size()) {
            result.addWeight(features.get(i1).name, features.get(i1).value);
            i1++;
        }

        while (i2 < v.features.size()) {
            result.addWeight(v.features.get(i2).name, v.features.get(i2).value);
            i2++;
        }

        return result;
    }

    public byte[] serialize() {
        BinaryBuffer buffer = new BinaryBuffer(getSerializedLength());

        buffer.append(identifier);
        buffer.append(features.size());

        for (FeatureValuePair pair : features) {
            buffer.append(pair.name);
            buffer.append((float) pair.value);
        }

        return buffer.getBuffer();
    }

    private int getSerializedLength() {
        int result = 0;

        result += BinaryBuffer.getSerializedLength(identifier);
        result += 4;

        for (FeatureValuePair pair : features) {
            result += BinaryBuffer.getSerializedLength(pair.name);
            result += 4;
        }

        return result;
    }

    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);
        identifier = buffer.readString();

        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            FeatureValuePair pair = new FeatureValuePair();
            pair.name = buffer.readString();
            pair.value = buffer.readFloat();

            features.add(pair);
        }
    }

    public void setLength(double length) {
        double coef = length / this.length;

        for (FeatureValuePair pair : features) {
            pair.value *= coef;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("edu.goergetown.bioasq.core.model.Vector:");
        result.append("\r\n    Identifier: " + identifier);
        result.append("\r\n    Features:");
        for (FeatureValuePair p : features) {
            result.append(String.format("\r\n        %s -> %.4f", p.name, p.value));
        }

        return result.toString();
    }
}

class FeatureValuePair {
    public String name = "";
    public double value = 0;
}