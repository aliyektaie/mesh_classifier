package edu.goergetown.bioasq.tasks.evaluation;

import edu.goergetown.bioasq.Constants;

public class EvaluationInputSource {
    public static final int TYPE_DEV = 1;
    public static final int TYPE_TEST = 2;

    public int type = 0;

    private EvaluationInputSource(int type) {
        this.type = type;
    }

    public static EvaluationInputSource TEST = new EvaluationInputSource(TYPE_TEST);
    public static EvaluationInputSource DEV = new EvaluationInputSource(TYPE_DEV);

    @Override
    public String toString() {
        return type == TYPE_DEV ? "Development" : "Test";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EvaluationInputSource) {
            return type == ((EvaluationInputSource)obj).type;
        }
        return false;
    }

    public String getFeatureSetFolder() {
        if (type == TYPE_DEV)
            return Constants.DEV_DOCUMENT_FEATURES_DATA_FOLDER;

        return Constants.TEST_DOCUMENT_FEATURES_DATA_FOLDER;
    }

    public String getFeatureSetFileName() {
        if (type == TYPE_DEV)
            return "dev_set.bin";

        return "test_set.bin";
    }
}
