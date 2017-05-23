package edu.goergetown.bioasq.utils;

import edu.goergetown.bioasq.Constants;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/15/17.
 */
public class DocumentListUtils {
    public static ArrayList<Integer> getAvailableDocumentYears() {
        ArrayList<String> files = FileUtils.getFiles(Constants.POS_DATA_FOLDER_BY_YEAR);
        ArrayList<Integer> result = new ArrayList<>();

        for (String path : files) {
            String year = path.substring(path.lastIndexOf(Constants.BACK_SLASH) + 1);
            year = year.substring(0, year.indexOf("-"));
            int y = Integer.valueOf(year);

            if (!result.contains(y)) {
                result.add(y);
            }
        }

        return result;
    }
}
