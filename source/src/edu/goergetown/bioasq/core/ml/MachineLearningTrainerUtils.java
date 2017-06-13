package edu.goergetown.bioasq.core.ml;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.model.FeatureValuePair;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.utils.FileUtils;

/**
 * Created by Yektaie on 6/4/2017.
 */
public class MachineLearningTrainerUtils {
    public static double[] getFeatureVector(String mesh, Vector vector) {
        String[] featuresName = FileUtils.readAllLines(Constants.TEXT_CLASSIFIER_FOLDER + "Features" + Constants.BACK_SLASH + mesh + ".txt");
        double[] result = new double[featuresName.length];

        for (int i = 0; i < vector.featureCount(); i++) {
            FeatureValuePair feature = vector.getFeature(i);
            int index = searchName(featuresName, feature.name);
            if (index >= 0) {
                result[index] = feature.value;
            }
        }

        return result;
    }

    private static int searchName(String[] featuresName, String name) {
        int begin = 0;
        int end = featuresName.length - 1;
        int middle = 0;

        name = name.toLowerCase();

        int c = 0;
        while (end - begin < 4 && c < 15) {
            middle = (end + begin) / 2;
            String m = featuresName[middle].toLowerCase();
            int cmp = m.compareTo(name);
            if (cmp == 0) {
                return middle;
            } else if (cmp > 0) {
                end = middle;
            } else {
                begin = middle;
            }
            c++;
        }

        for (int i = begin; i <= end; i++) {
            if (featuresName[i].toLowerCase().equals(name)) {
                return i;
            }
        }

        return -1;
    }
}
