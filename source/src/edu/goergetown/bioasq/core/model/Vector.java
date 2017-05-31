package edu.goergetown.bioasq.core.model;

import edu.goergetown.bioasq.utils.BinaryBuffer;

import java.util.*;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class Vector {
    public final static int SIMILARITY_COSINE = 1;
    public final static int SIMILARITY_JACCARD = 2;

    private final int INITIAL_SIZE = 50;
    private final int GROWTH_SIZE = 50;

    public FeatureValuePair[] features = new FeatureValuePair[INITIAL_SIZE];
    public int featureCount = 0;

    public String identifier = "";
    private boolean optimized = false;
    private double length = -1;

    private Hashtable<String, FeatureValuePair> checkForDuplicateHelper = null;

    public void addWeight(String name, double weight) {
        if (checkForDuplicateHelper != null) {
            if (checkForDuplicateHelper.containsKey(name)) {
                checkForDuplicateHelper.get(name).value = checkForDuplicateHelper.get(name).value + weight;
                return;
            }
        }

        FeatureValuePair pair = new FeatureValuePair();

        pair.name = name;
        pair.value = weight;

        addFeature(pair);

        if (checkForDuplicateHelper != null) {
            checkForDuplicateHelper.put(name, pair);
        }

        optimized = false;
    }

    private void addFeature(FeatureValuePair pair) {
        if (featureCount + 1 == features.length) {
            FeatureValuePair[] temp = new FeatureValuePair[features.length + GROWTH_SIZE];
            System.arraycopy(features, 0, temp, 0, features.length);

            features = temp;
        }

        features[featureCount] = pair;
        featureCount++;
    }

    public double getSimilarity(Vector v, int type) {
        try {
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
        } catch (NullPointerException ex) {
            return getSimilarity(v, type);
        }
    }

    private double similarityJaccard(Vector v) {
        int a_b = 0;
        int a = 0;
        int b = 0;

        int i1 = 0;
        int i2 = 0;

        while (i1 < featureCount && i2 < v.featureCount) {
            FeatureValuePair f1 = features[i1];
            FeatureValuePair f2 = v.features[i2];

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

        return (a_b * 1.0) / (a + b - a_b);
    }

    private double similarityCosine(Vector v) {
        double dotProduct = 0;
        int i1 = 0;
        int i2 = 0;

        while (i1 < featureCount && i2 < v.featureCount) {
            FeatureValuePair f1 = features[i1];
            FeatureValuePair f2 = v.features[i2];

            int cmp = f1.name.compareTo(f2.name);
            if (cmp == 0) {
                dotProduct += (f1.value * f2.value);

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
        Comparator<? super FeatureValuePair> comparator = new Comparator<FeatureValuePair>() {
            @Override
            public int compare(FeatureValuePair f1, FeatureValuePair f2) {
                return f1.name.compareTo(f2.name);
            }
        };

        Arrays.sort(features, 0, featureCount, comparator);
        double l = 0;

        for (int i = 0; i < featureCount; i++) {
            l += Math.pow(features[i].value, 2);
        }

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

        while (i1 < featureCount && i2 < v.featureCount) {
            FeatureValuePair f1 = features[i1];
            FeatureValuePair f2 = features[i2];

            int cmp = f1.name.compareTo(f2.name);
            if (cmp == 0) {
                result.addWeight(f1.name, f1.value + f2.value);

                i1++;
                i2++;
            } else if (cmp < 0) {
                result.addWeight(f1.name, f1.value);
                i1++;
            } else {
                result.addWeight(f2.name, f2.value);
                i2++;
            }
        }

        while (i1 < featureCount) {
            result.addWeight(features[i1].name, features[i1].value);
            i1++;
        }

        while (i2 < v.featureCount) {
            result.addWeight(v.features[i2].name, v.features[i2].value);
            i2++;
        }

        return result;
    }

    public byte[] serialize() {
        BinaryBuffer buffer = new BinaryBuffer(getSerializedLength());

        buffer.append(identifier);
        buffer.append(featureCount);

        for (int i = 0; i < featureCount; i++) {
            FeatureValuePair pair = features[i];

            buffer.append(pair.name);
            buffer.append((float) pair.value);
        }

        return buffer.getBuffer();
    }

    public int getSerializedLength() {
        int result = 0;

        result += BinaryBuffer.getSerializedLength(identifier);
        result += 4;

        for (int i = 0; i < featureCount; i++) {
            FeatureValuePair pair = features[i];

            result += BinaryBuffer.getSerializedLength(pair.name);
            result += 4;
        }

        return result;
    }

    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);
        identifier = buffer.readString();

        int count = buffer.readInt();
        features = new FeatureValuePair[count];
        featureCount = 0;

        for (int i = 0; i < count; i++) {
            FeatureValuePair pair = new FeatureValuePair();
            pair.name = buffer.readString();
            pair.value = buffer.readFloat();

            addFeature(pair);
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

    public void checkForAddDuplicates() {
        checkForDuplicateHelper = new Hashtable<>();

        for (FeatureValuePair pair : features) {
            checkForDuplicateHelper.put(pair.name, pair);
        }
    }
}

//public class Vector {
//    public final static int SIMILARITY_COSINE = 1;
//    public final static int SIMILARITY_JACCARD = 2;
//
//    public ArrayList<FeatureValuePair> features = new ArrayList<>();
//    public String identifier = "";
//    private boolean optimized = false;
//    private double length = -1;
//
//    private Hashtable<String, FeatureValuePair> checkForDuplicateHelper = null;
//
//    public void addWeight(String name, double weight) {
//        if (checkForDuplicateHelper != null) {
//            if (checkForDuplicateHelper.containsKey(name)) {
//                checkForDuplicateHelper.get(name).value = checkForDuplicateHelper.get(name).value + weight;
//                return;
//            }
//        }
//
//        FeatureValuePair pair = new FeatureValuePair();
//
//        pair.name = name;
//        pair.value = weight;
//
//        features.add(pair);
//
//        if (checkForDuplicateHelper != null) {
//            checkForDuplicateHelper.put(name, pair);
//        }
//
//        optimized = false;
//    }
//
//    public double getSimilarity(Vector v, int type) {
//        try {
//            if (!optimized) {
//                optimize();
//            }
//
//            if (!v.optimized)
//                v.optimize();
//
//            double result = 0;
//
//            switch (type) {
//                case SIMILARITY_COSINE:
//                    result = similarityCosine(v);
//                    break;
//                case SIMILARITY_JACCARD:
//                    result = similarityJaccard(v);
//                    break;
//            }
//
//            return result;
//        } catch (NullPointerException ex) {
//            return getSimilarity(v, type);
//        }
//    }
//
//    private double similarityJaccard(Vector v) {
//        int a_b = 0;
//        int a = 0;
//        int b = 0;
//
//        int i1 = 0;
//        int i2 = 0;
//
//        while (i1 < features.size() && i2 < v.features.size()) {
//            FeatureValuePair f1 = features.get(i1);
//            FeatureValuePair f2 = v.features.get(i2);
//
//            int cmp = f1.name.compareTo(f2.name);
//            if (cmp == 0) {
//                a_b++;
//                a++;
//                b++;
//
//                i1++;
//                i2++;
//            } else if (cmp < 0) {
//                i1++;
//
//                a++;
//            } else {
//                i2++;
//
//                b++;
//            }
//        }
//
//        return (a_b * 1.0)/ (a + b - a_b);
//    }
//
//    private double similarityCosine(Vector v) {
//        double dotProduct = 0;
//        int i1 = 0;
//        int i2 = 0;
//
//        while (i1 < features.size() && i2 < v.features.size()) {
//            FeatureValuePair f1 = features.get(i1);
//            FeatureValuePair f2 = v.features.get(i2);
//
//            if (f1 == null) {
//                i1++;
//                continue;
//            }
//
//            if (f2 == null) {
//                i2++;
//                continue;
//            }
//
//            int cmp = f1.name.compareTo(f2.name);
//            if (cmp == 0) {
//                dotProduct += (features.get(i1).value * v.features.get(i2).value);
//
//                i1++;
//                i2++;
//            } else if (cmp < 0) {
//                i1++;
//            } else {
//                i2++;
//            }
//        }
//
//        return dotProduct / (length * v.length);
//    }
//
//    public void optimize() {
//        try {
//            FeatureValuePair[] features = new FeatureValuePair[this.features.size()];
//            double l = 0;
//
//            for (int i = 0; i < features.length; i++) {
//                features[i] = this.features.get(i);
//            }
//
//            Arrays.sort(features, new Comparator<FeatureValuePair>() {
//                @Override
//                public int compare(FeatureValuePair f1, FeatureValuePair f2) {
//                    if (f1 == null && f2 == null)
//                        return 0;
//
//                    if (f1 == null)
//                        return -1;
//
//                    if (f2 == null)
//                        return 1;
//                    return f1.name.compareTo(f2.name);
//                }
//            });
//
//            this.features.clear();
//            for (int i = 0; i < features.length; i++) {
//                if (features[i]!=null) {
//                    this.features.add(features[i]);
//                    l = l + (features[i].value * features[i].value);
//                }
//            }
//
//            this.length = Math.sqrt(l);
//
//            optimized = true;
//        } catch (NullPointerException ex) {
//            ex.printStackTrace();
//            optimize();
//        }
//    }
//
//    public Vector add(Vector v) {
//        Vector result = new Vector();
//
//        if (!optimized) {
//            optimize();
//        }
//
//        if (!v.optimized)
//            v.optimize();
//
//        int i1 = 0;
//        int i2 = 0;
//
//        while (i1 < features.size() && i2 < v.features.size()) {
//            String f1 = features.get(i1).name;
//            String f2 = v.features.get(i2).name;
//
//            int cmp = f1.compareTo(f2);
//            if (cmp == 0) {
//                result.addWeight(f1, features.get(i1).value + v.features.get(i2).value);
//
//                i1++;
//                i2++;
//            } else if (cmp < 0) {
//                result.addWeight(f1, features.get(i1).value);
//                i1++;
//            } else {
//                result.addWeight(f2, v.features.get(i2).value);
//                i2++;
//            }
//        }
//
//        while (i1 < features.size()) {
//            result.addWeight(features.get(i1).name, features.get(i1).value);
//            i1++;
//        }
//
//        while (i2 < v.features.size()) {
//            result.addWeight(v.features.get(i2).name, v.features.get(i2).value);
//            i2++;
//        }
//
//        return result;
//    }
//
//    public byte[] serialize() {
//        BinaryBuffer buffer = new BinaryBuffer(getSerializedLength());
//
//        buffer.append(identifier);
//        buffer.append(features.size());
//
//        for (FeatureValuePair pair : features) {
//            buffer.append(pair.name);
//            buffer.append((float) pair.value);
//        }
//
//        return buffer.getBuffer();
//    }
//
//    public int getSerializedLength() {
//        int result = 0;
//
//        result += BinaryBuffer.getSerializedLength(identifier);
//        result += 4;
//
//        for (FeatureValuePair pair : features) {
//            result += BinaryBuffer.getSerializedLength(pair.name);
//            result += 4;
//        }
//
//        return result;
//    }
//
//    public void load(byte[] data) {
//        BinaryBuffer buffer = new BinaryBuffer(data);
//        identifier = buffer.readString();
//
//        int count = buffer.readInt();
//        for (int i = 0; i < count; i++) {
//            FeatureValuePair pair = new FeatureValuePair();
//            pair.name = buffer.readString();
//            pair.value = buffer.readFloat();
//
//            features.add(pair);
//        }
//    }
//
//    public void setLength(double length) {
//        double coef = length / this.length;
//
//        for (FeatureValuePair pair : features) {
//            pair.value *= coef;
//        }
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder result = new StringBuilder();
//
//        result.append("edu.goergetown.bioasq.core.model.Vector:");
//        result.append("\r\n    Identifier: " + identifier);
//        result.append("\r\n    Features:");
//        for (FeatureValuePair p : features) {
//            result.append(String.format("\r\n        %s -> %.4f", p.name, p.value));
//        }
//
//        return result.toString();
//    }
//
//    public void checkForAddDuplicates() {
//        checkForDuplicateHelper = new Hashtable<>();
//
//        for (FeatureValuePair pair : features) {
//            checkForDuplicateHelper.put(pair.name, pair);
//        }
//    }
//}
//
