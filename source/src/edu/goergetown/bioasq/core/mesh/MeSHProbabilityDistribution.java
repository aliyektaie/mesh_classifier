package edu.goergetown.bioasq.core.mesh;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.Hashtable;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class MeSHProbabilityDistribution {
    private static Hashtable<String, Double> probabilities = new Hashtable<>();
    private static double minimumProbability = 1.0;
    private static double maximumProbability = 0;

    static {
        String path = Constants.MESH_INFO_FOLDER_BY_YEAR + "single_mesh_probability_distribution.txt";
        String[] lines = FileUtils.readAllLines(path);

        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].trim().split(" -> ");
            Double prob = Double.valueOf(parts[1]);
            probabilities.put(parts[0], prob);

            if (minimumProbability > prob) {
                minimumProbability = prob;
            }

            if (maximumProbability < prob) {
                maximumProbability = prob;
            }
        }
    }

    public static double getProbability(String mesh) {
        return probabilities.get(mesh);
    }

    public static double getMinimumProbability() {
        return minimumProbability;
    }

    public static double getMaximumProbability() {
        return maximumProbability;
    }
}
