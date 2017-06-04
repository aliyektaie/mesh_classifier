package edu.goergetown.bioasq;

import edu.goergetown.bioasq.utils.FileUtils;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class Constants {
    public static void initialize() {
        if (System.getProperty("os.name").toLowerCase().equals("linux")) {
            BACK_SLASH = "/";
            ROOT_FOLDER = "/media/yektaie/Files/";
            STANFORD_POS_MODEL_FILE_PATH = "/media/yektaie/Files/Projects/MeSH/mesh_classifier/source/lib/stanford-postagger/models/english-bidirectional-distsim.tagger";
        } else {
            BACK_SLASH = "\\";
            if (isLaptop()) {
                ROOT_FOLDER = "e:\\";
                STANFORD_POS_MODEL_FILE_PATH = "D:\\Projects\\MeSH\\mesh_classifier\\source\\lib\\stanford-postagger\\models\\english-bidirectional-distsim.tagger";
            } else {
                ROOT_FOLDER = "c:\\Users\\zghasemi\\Desktop\\Ali\\";
                STANFORD_POS_MODEL_FILE_PATH = "c:\\Users\\zghasemi\\Desktop\\Ali\\mesh_classifier\\source\\lib\\stanford-postagger\\models\\english-bidirectional-distsim.tagger";
                CORE_COUNT = 8;
                UI_COEF = 1;
            }
        }

        UMLS_INSTALLATION_LOCATION = ROOT_FOLDER + "UMLS" + BACK_SLASH;
        DATA_FOLDER = ROOT_FOLDER + "data" + BACK_SLASH;

        CLASSIFIERS_CONFIGURATIONS_FOLDER = DATA_FOLDER + "Configurations" + BACK_SLASH;
        SETS_DATA_FOLDER = DATA_FOLDER + "Sets" + BACK_SLASH;
        CLASSIFIERS_MODELS_FOLDER = DATA_FOLDER + "Classification Models" + BACK_SLASH;
        DATA_FOLDER_BY_YEAR = DATA_FOLDER + "By Year" + BACK_SLASH;
        MESH_INFO_FOLDER_BY_YEAR = DATA_FOLDER + "MeSH Info" + BACK_SLASH;
        CLUSTERING_DOCUMENT_MESH_INFO_FOLDER = DATA_FOLDER + "Document MeSH Clusters" + BACK_SLASH;
        POS_DATA_FOLDER_BY_YEAR = DATA_FOLDER + "POS By Year" + BACK_SLASH;
        REPORTS_FOLDER = DATA_FOLDER + "Reports" + BACK_SLASH;
        TRAIN_DOCUMENT_FEATURES_DATA_FOLDER = DATA_FOLDER + "Document Features" + BACK_SLASH;
        DEV_DOCUMENT_FEATURES_DATA_FOLDER = SETS_DATA_FOLDER + "Dev Document Features" + BACK_SLASH;
        TEST_DOCUMENT_FEATURES_DATA_FOLDER = SETS_DATA_FOLDER + "Test Document Features" + BACK_SLASH;
        TEMP_FOLDER = DATA_FOLDER + "Temp" + BACK_SLASH;
        TEXT_CLASSIFIER_FOLDER = DATA_FOLDER + "Text Classifiers" + BACK_SLASH;
        ORIGINAL_DATA_FILE = String.format("%s%s%s%s", DATA_FOLDER, "Original Data File", BACK_SLASH, "allMeSH_2017.json");

    }

    public static int CORE_COUNT = 4;
    public static double UI_COEF = 2.15;

    public static String BACK_SLASH = "";
    private static String ROOT_FOLDER = "";
    public static String SETS_DATA_FOLDER = "";
    public static String STANFORD_POS_MODEL_FILE_PATH = "";
    public static String UMLS_INSTALLATION_LOCATION = "";
    public static String DATA_FOLDER = "";
    public static String DATA_FOLDER_BY_YEAR = "";
    public static String MESH_INFO_FOLDER_BY_YEAR = "";
    public static String CLUSTERING_DOCUMENT_MESH_INFO_FOLDER = "";
    public static String POS_DATA_FOLDER_BY_YEAR = "";
    public static String REPORTS_FOLDER = "";
    public static String TEXT_CLASSIFIER_FOLDER = "";
    public static String TRAIN_DOCUMENT_FEATURES_DATA_FOLDER = "";
    public static String DEV_DOCUMENT_FEATURES_DATA_FOLDER = "";
    public static String TEST_DOCUMENT_FEATURES_DATA_FOLDER = "";
    public static String ORIGINAL_DATA_FILE = "";
    public static String TEMP_FOLDER = "";
    public static String CLASSIFIERS_CONFIGURATIONS_FOLDER = "";
    public static String CLASSIFIERS_MODELS_FOLDER = "";

    public static String getDataFolder(int year) {
        return String.format("%s%s%s%d%s", DATA_FOLDER, "By Year", BACK_SLASH, year, BACK_SLASH);
    }

    public static boolean isLaptop() {
        return !FileUtils.exists("c:\\Users\\zghasemi\\Desktop\\Ali\\");
    }
}
