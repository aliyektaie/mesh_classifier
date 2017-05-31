package edu.goergetown.bioasq.core.model.classification;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

public class ClassifierParameter {
    public String clusteringMethod = "";
    public String clusteringParameter = "";
    public String termExtractionMethod = "";

    @Override
    public String toString() {
        return String.format("%s [%s] [%s]", clusteringMethod, clusteringParameter, termExtractionMethod);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassifierParameter) {
            ClassifierParameter p = (ClassifierParameter) obj;

            return clusteringMethod.equals(p.clusteringMethod) &&
                    clusteringParameter.equals(p.clusteringParameter) &&
                    termExtractionMethod.equals(p.termExtractionMethod);
        }

        return false;
    }

    public static ClassifierParameter load(String path) {
        String[] lines = FileUtils.readAllLines(path);
        ClassifierParameter result = new ClassifierParameter();

        for (int i = 0; i < lines.length; i++) {
            String line =lines[i];
            String key = line.substring(0, line.indexOf(":"));
            String value = line.substring(line.indexOf(":") + 1).trim();

            switch (key) {
                case "clusteringMethod":
                    result.clusteringMethod = value;
                    break;
                case "clusteringParameter":
                    result.clusteringParameter = value;
                    break;
                case "termExtractionMethod":
                    result.termExtractionMethod = value;
                    break;
            }
        }

        return result;
    }

    public static ArrayList<ClassifierParameter> getConfigurations() {
        ArrayList<String> files = FileUtils.getFiles(Constants.CLASSIFIERS_CONFIGURATIONS_FOLDER);
        ArrayList<ClassifierParameter> result = new ArrayList<>();

        for (String path : files) {
            result.add(ClassifierParameter.load(path));
        }

        return result;
    }

}
