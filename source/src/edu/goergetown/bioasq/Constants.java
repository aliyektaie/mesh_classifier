package edu.goergetown.bioasq;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class Constants {
    public static void initialize() {
        if (System.getProperty("os.name").toLowerCase().equals("linux")) {
            BACK_SLASH = "/";
            ROOT_FOLDER = "/media/yektaie/External/";
        } else {
            BACK_SLASH = "\\";
            ROOT_FOLDER = "E:\\";
        }

        STANFORD_POS_MODEL_FILE_PATH = ROOT_FOLDER + "mesh_classifier" + BACK_SLASH + "source" + BACK_SLASH + "lib" + BACK_SLASH + "stanford-postagger" + BACK_SLASH + "models" + BACK_SLASH + "english-bidirectional-distsim.tagger";

        UMLS_INSTALLATION_LOCATION = ROOT_FOLDER + "UMLS" + BACK_SLASH;

        DATA_FOLDER = ROOT_FOLDER + "data" + BACK_SLASH;
        DATA_FOLDER_BY_YEAR = DATA_FOLDER + "By Year" + BACK_SLASH;
        MESH_INFO_FOLDER_BY_YEAR = DATA_FOLDER + "MeSH Info" + BACK_SLASH;
        CLUSTERING_DOCUMENT_MESH_INFO_FOLDER = DATA_FOLDER + "Document MeSH Clusters" + BACK_SLASH;
        POS_DATA_FOLDER_BY_YEAR = DATA_FOLDER + "POS By Year" + BACK_SLASH;
        DOCUMENT_FEATURES_DATA_FOLDER = DATA_FOLDER + "Document Features" + BACK_SLASH;
        TEMP_FOLDER = DATA_FOLDER + "Temp" + BACK_SLASH;
        ORIGINAL_DATA_FILE = String.format("%s%s%s%s", DATA_FOLDER, "Original Data File", BACK_SLASH, "allMeSH_2017.json");

    }

    public static String BACK_SLASH = "";
    private static String ROOT_FOLDER = "";
    public static String STANFORD_POS_MODEL_FILE_PATH = "";
    public static String UMLS_INSTALLATION_LOCATION = "";
    public static String DATA_FOLDER = "";
    public static String DATA_FOLDER_BY_YEAR = "";
    public static String MESH_INFO_FOLDER_BY_YEAR = "";
    public static String CLUSTERING_DOCUMENT_MESH_INFO_FOLDER = "";
    public static String POS_DATA_FOLDER_BY_YEAR = "";
    public static String DOCUMENT_FEATURES_DATA_FOLDER = "";
    public static String ORIGINAL_DATA_FILE = "";
    public static String TEMP_FOLDER = "";
    public static int CORE_COUNT = 4;

    public static String getDataFolder(int year) {
        return String.format("%s%s%s%d%s", DATA_FOLDER, "By Year", BACK_SLASH, year, BACK_SLASH);
    }
}
