package edu.goergetown.bioasq;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class Constants {
    public static final String DATA_FOLDER = "D:\\Projects\\BioASQ\\mesh_classifier\\data\\";
    public static final String DATA_FOLDER_BY_YEAR = "D:\\Projects\\BioASQ\\mesh_classifier\\data\\By Year\\";
    public static final String BACK_SLASH = "\\";
    public static final String ORIGINAL_DATA_FILE = String.format("%s%s%s%s", DATA_FOLDER, "Original Data File", BACK_SLASH, "allMeSH_2017.json");
    public static final int NUMBER_OF_FILE_IN_DATASET_FOLDER = 5000;

    public static String getDataFolder(int year) {
        return String.format("%s%s%s%d%s",DATA_FOLDER, "By Year", BACK_SLASH, year, BACK_SLASH);
    }
}
