package edu.goergetown.bioasq.core.umls;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.feature_extractor.implementations.TopicRankFeatureExtractor;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Created by yektaie on 5/17/17.
 */
public class UMLSDictionary {
    private Hashtable<String, ArrayList<String>> terms = new Hashtable<>();

    public static UMLSDictionary loadDictionary(ITaskListener listener) {
        UMLSDictionary result = new UMLSDictionary();

        String rawDictionaryPath = Constants.UMLS_INSTALLATION_LOCATION + "raw_dictionary.txt";
        if (FileUtils.exists(rawDictionaryPath)) {
            result.loadRaw(rawDictionaryPath, listener);
        } else {
            extractEntriesFromOriginalFile(listener, result);
            result.saveRaw(rawDictionaryPath);
        }

        postProcessDictionary(result);

        return result;
    }

    private void loadRaw(String path, ITaskListener listener) {
        listener.setCurrentState("Loading UMLS dictionary");

        try {
            int fileLength = FileUtils.getFileLength(path);
            int readLength = 0;
            int lineReadCount = 0;

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));

            String line = null;
            String key = "";

            while ((line = br.readLine()) != null) {
                readLength += (line.length() + 1);
                lineReadCount++;

                if (line.trim().length() > 0) {
                    updateProgress(listener, fileLength, readLength, lineReadCount);

                    if (!line.startsWith("\t")) {
                        terms.put(line, new ArrayList<>());
                        key = line;
                    } else {
                        terms.get(key).add(line.trim());
                    }
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveRaw(String path) {
        StringBuilder result = new StringBuilder();

        String del = "";
        for (String key : terms.keySet()) {
            result.append(del);
            del= "\n";
            result.append(key);

            String[] temp = new String[terms.get(key).size()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = terms.get(key).get(i);
            }

            Arrays.sort(temp);

            for (int i = 0; i < temp.length; i++) {
                result.append("\n\t");
                result.append(temp[i]);
            }
        }

        FileUtils.writeText(path, result.toString());
    }

    private static void extractEntriesFromOriginalFile(ITaskListener listener, UMLSDictionary result) {
        listener.setCurrentState("Extracting vocabularies from UMLS dictionary");

        try {
            String umlsDictionaryPath = Constants.UMLS_INSTALLATION_LOCATION + "META" + Constants.BACK_SLASH + "MRCONSO.RRF";

            int fileLength = FileUtils.getFileLength(umlsDictionaryPath);
            int readLength = 0;
            int lineReadCount = 0;

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(umlsDictionaryPath), "UTF8"));

            String line = null;

            while ((line = br.readLine()) != null) {
                readLength += (line.length() + 1);
                lineReadCount++;

                if (line.trim().length() > 0) {
                    updateProgress(listener, fileLength, readLength, lineReadCount);

                    String[] parts = line.replace("|", "&&&&&&").split("&&&&&&");
                    String language = parts[1];
                    if (language.equals("ENG")) {
                        addTerm(result, parts[14]);
                    }
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void postProcessDictionary(UMLSDictionary result) {

    }

    private static void addTerm(UMLSDictionary dictionary, String term) {
        term = term.toLowerCase();

        String key = term.substring(0, Math.min(3, term.length()));
        if (!dictionary.terms.containsKey(key)) {
            dictionary.terms.put(key, new ArrayList<>());
        }

        if (!dictionary.terms.get(key).contains(term)) {
            dictionary.terms.get(key).add(term);
        }
    }

    private static void updateProgress(ITaskListener listener, int fileLength, int readLength, int lineReadCount) {
        if (lineReadCount % 100 == 0) {
            listener.setProgress(readLength, fileLength);
        }
    }

    public ArrayList<String> match(String word) {
        word= word.toLowerCase();
        ArrayList<String> result = new ArrayList<>();
        String key = word.substring(0, Math.min(3, word.length()));
        ArrayList<String> words = terms.get(key);

        if (words != null) {
            int begin = 0;
            int end = words.size() - 1;

            while (end - begin > 10) {
                int middle = (end + begin) / 2;
                int compared = words.get(middle).compareTo(word);
                if (compared == 0) {
                    begin = Math.max(middle - 5, 0);
                    end = Math.min(middle + 5, words.size() - 1);
                } else if (compared < 0) {
                    begin = middle;
                } else {
                    end = middle;
                }
            }

            for (int i = begin; i <= end; i++) {
                String w = words.get(i);
                if (w.equals(word)) {
                    result.add(w);
                }
            }

            if (result.size() == 0) {
                for (int i = begin; i <= end; i++) {
                    String w = words.get(i);
                    if (TopicRankFeatureExtractor.getSimilarity(word, w) > 30.0) {
                        result.add(w);
                    }
                }
            }
        }

        return result;
    }
}
