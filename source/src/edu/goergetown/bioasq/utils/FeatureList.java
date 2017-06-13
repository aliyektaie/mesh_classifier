package edu.goergetown.bioasq.utils;

import edu.goergetown.bioasq.core.model.FeatureValuePair;

import java.util.ArrayList;

/**
 * Created by zghasemi on 6/4/2017.
 */
public class FeatureList {
    private static final int INITIAL_SIZE = 50;
    private static final int INC_SIZE = 20;

    private FeatureValuePair[] values = null;
    private int length = 0;
    private FeatureValuePair[] asArrayCache = null;

    public FeatureList() {
        values = new FeatureValuePair[INITIAL_SIZE];
    }

    public FeatureList(int featureCount) {
        values = new FeatureValuePair[featureCount];
    }

    public synchronized int length() {
        return length;
    }

    public synchronized void add(FeatureValuePair pair) {
        this.asArrayCache = null;

        if (length == values.length) {
            incrementSize();
        }

//        boolean contains = hasFeature(pair.name);
//        if (contains) {
//            throw new RuntimeException();
//        }

        values[length] = pair;
        length++;
    }

    private boolean hasFeature(String name) {
        boolean result = false;

        for (int i = 0; i < length; i++) {
            if (values[i].name.equals(name)) {
                result = false;
            }
        }

        return result;
    }

    private synchronized void incrementSize() {
        FeatureValuePair[] temp = new FeatureValuePair[values.length + INC_SIZE];
        for (int i = 0; i < values.length; i++) {
            temp[i] = values[i];
        }

        values = temp;
    }

    public synchronized void set(int i, FeatureValuePair pair) {
        this.asArrayCache = null;

        if (i < length) {
            values[i] = pair;
        } else {
            throw new RuntimeException();
        }
    }

    public synchronized FeatureValuePair get(int i) {
        if (i < length) {
            return values[i];
        } else {
            throw new RuntimeException();
        }
    }

    public synchronized void clear() {
        this.asArrayCache = null;

        for (int i = 0; i < length; i++) {
            values[i] = null;
        }

        length = 0;
    }

    public synchronized ArrayList<FeatureValuePair> asList() {
        ArrayList<FeatureValuePair> result = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            result.add(values[i]);
        }

        return result;
    }

    public synchronized FeatureValuePair[] asArray() {
        if (this.asArrayCache != null) {
            return this.asArrayCache;
        }

        FeatureValuePair[] result = new FeatureValuePair[length];

        for (int i = 0; i < result.length; i++) {
            result[i] = values[i];
        }

        this.asArrayCache = result;

        return result;
    }
}
