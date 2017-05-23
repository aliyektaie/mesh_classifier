package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.DocumentListUtils;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/18/2017.
 */
public class CreateMeSHProbabilityDistributionTask extends BaseTask {
    private SubTaskInfo PROBABILITY_CALCULATOR_TASK = new SubTaskInfo("Calculate MeSH conditional probability", 10);
    private Hashtable<String, Integer> andProbabilities = new Hashtable<>();
    private Hashtable<String, Integer> singleProbabilities = new Hashtable<>();

    @Override
    public void process(ITaskListener listener) {
        ArrayList<Integer> years = DocumentListUtils.getAvailableDocumentYears();
        final int[] count = {0};
        int total = getTotalDocumentCount(years);

        for (int yearIndex = 0; yearIndex < years.size(); yearIndex++) {
            int year = years.get(yearIndex);
            for (int core = 0; core < Constants.CORE_COUNT; core++) {
                String path = Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + core + ".bin";
                Document.iterateOnDocumentListFile(path, new IDocumentLoaderCallback() {
                    @Override
                    public void processDocument(Document document, int i, int totalDocumentCount) {
                        count[0]++;
                        if (count[0] % 1000 == 0) {
                            updateProgress(listener, PROBABILITY_CALCULATOR_TASK, count[0], total);
                        }

                        processMeSHs(document.categories);
                    }
                });
            }
        }

        saveDistribution(listener, singleProbabilities, "single_mesh_probability_distribution", total);
        saveDistribution(listener, andProbabilities, "combination_mesh_probability_distribution", total);
    }

    private void saveDistribution(ITaskListener listener, Hashtable<String, Integer> hashtable, String fileName, double totalDocumentCount) {
        listener.log("Sorting probabilities for " + fileName);
        StringDoublePair[] values = new StringDoublePair[hashtable.size()];
        int i = 0;

        for (String mesh : hashtable.keySet()) {
            StringDoublePair pair = new StringDoublePair();
            pair.name = mesh;
            pair.value = hashtable.get(mesh) * 1.0 / totalDocumentCount;

            values[i] = pair;
            i++;
        }

        Arrays.sort(values, new Comparator<StringDoublePair>() {
            @Override
            public int compare(StringDoublePair o1, StringDoublePair o2) {
                return new Double(o2.value).compareTo(o1.value);
            }
        });

        if (!FileUtils.exists(Constants.MESH_INFO_FOLDER_BY_YEAR)) {
            FileUtils.createDirectory(Constants.MESH_INFO_FOLDER_BY_YEAR);
        }

        saveDistributionFile(fileName, totalDocumentCount, values);
    }

    private void saveDistributionFile(String fileName, double totalDocumentCount, StringDoublePair[] values) {
        String path = Constants.MESH_INFO_FOLDER_BY_YEAR + fileName + ".txt";
        try {
            FileOutputStream fs = new FileOutputStream(path);
            String del = "";
            for (int j = 0; j < values.length; j++) {
                StringDoublePair pair = values[j];
                fs.write(del.getBytes());
                del = "\r\n";
                String line = pair.name + " -> " + pair.value;
                fs.write(line.getBytes());

                if (pair.name.contains("___")) {
                    String a = pair.name.split("___")[0];
                    String b = pair.name.split("___")[1];

                    StringBuilder result = new StringBuilder();

                    result.append("\r\n\t");
                    result.append("P(" + a + "|" + b + ") = ");
                    result.append(String.format("%.5f", pair.value * 1.0 / (singleProbabilities.get(b) * 1.0 / totalDocumentCount)));
                    result.append("\r\n\t");
                    result.append("P(" + b + "|" + a + ") = ");
                    result.append(String.format("%.5f", pair.value * 1.0 / (singleProbabilities.get(a) * 1.0 / totalDocumentCount)));
                    del = "\r\n\r\n";

                    fs.write(result.toString().getBytes());
                }
            }

            fs.flush();
            fs.close();
        } catch (Exception e) {

        }
    }

    private void processMeSHs(ArrayList<String> categories) {
        ArrayList<String> processed = new ArrayList<>();
        for (String a : categories) {
            for (String b : categories) {
                if (!a.equals(b)) {
                    String key = getKeyForMeSHPair(a, b);
                    if (!processed.contains(key)) {
                        incrementCount(key, andProbabilities);
                        processed.add(key);
                    }

                    if (!processed.contains(a)) {
                        incrementCount(a, singleProbabilities);
                        processed.add(a);
                    }

                    if (!processed.contains(b)) {
                        incrementCount(b, singleProbabilities);
                        processed.add(b);
                    }
                }
            }
        }
    }

    private void incrementCount(String mesh, Hashtable<String, Integer> hashTable) {
        if (hashTable.containsKey(mesh)) {
            int count = hashTable.get(mesh);
            hashTable.put(mesh, count + 1);
        } else {
            hashTable.put(mesh, 1);
        }
    }

    private String getKeyForMeSHPair(String a, String b) {
        String result = a + "___" + b;

        if (a.compareTo(b) > 0) {
            result = b + "___" + a;
        }

        return result;
    }

    private int getTotalDocumentCount(ArrayList<Integer> years) {
        int result = 0;

        for (int yearIndex = 0; yearIndex < years.size(); yearIndex++) {
            int year = years.get(yearIndex);
            for (int core = 0; core < Constants.CORE_COUNT; core++) {
                String path = Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + core + ".bin";
                result += Document.getDocumentCountInFile(path);
            }
        }

        return result;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Create MeSH Probability Distribution";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(PROBABILITY_CALCULATOR_TASK);

        return result;
    }
}

class StringDoublePair {
    public String name = "";
    public double value = 0;
}