package edu.goergetown.bioasq.core.predictor;

import edu.goergetown.bioasq.core.predictor.implementations.ClusterAccuracyEvaluationPredictor;

import java.util.ArrayList;

/**
 * Created by zghasemi on 5/31/2017.
 */
public class MeshPredictors {
    private static ArrayList<IMeshPredictor> predictors = null;

    static {
        predictors = new ArrayList<>();

        predictors.add(new ClusterAccuracyEvaluationPredictor());
    }

    public static ArrayList<IMeshPredictor> getPredictors() {
        return predictors;
    }
}
