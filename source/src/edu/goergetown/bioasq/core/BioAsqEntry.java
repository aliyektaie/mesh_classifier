package edu.goergetown.bioasq.core;

import com.google.gson.GsonBuilder;
import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

/**
 * Created by yektaie on 4/18/17.
 */
public class BioAsqEntry {
    public String journal = "";
    public ArrayList<String> meshMajor = new ArrayList<>();
    public int year = 0;
    public String abstractText = "";
    public String pmid = "";
    public String title = "";

    public void save() {
        String path = String.format("%s%s%s",Constants.getDataFolder(year),pmid.substring(pmid.length() - 3, pmid.length()),Constants.BACK_SLASH);

        if (!FileUtils.exists(path)) {
            FileUtils.createDirectory(path);
        }

        path = path + (pmid + ".txt");
        save(path);
    }

    public void save(String path) {
        String json = serialize();
        FileUtils.writeText(path, json);
    }

    private String serialize() {
        StringBuilder result = new StringBuilder();

        result.append(String.format("%s ---> %s", "journal", journal));
        result.append(String.format("\n%s ---> %s", "year", String.valueOf(year)));
        result.append(String.format("\n%s ---> %s", "title", title));
        result.append(String.format("\n%s ---> %s", "pmid", pmid));
        result.append(String.format("\n%s ---> %s", "abstractText", abstractText.replace("\r", "\\r").replace("\n", "\\n")));
        for (String mesh : meshMajor) {
            result.append(String.format("\n%s ---> %s", "mesh", mesh));
        }

        return result.toString();
    }
}
