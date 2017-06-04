package edu.goergetown.bioasq.tasks.experiments;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/31/2017.
 */
public class MeSHLabelTermOccurrenceInDocumentTask extends BaseTask {
    private SubTaskInfo PROCESSING_TASK = new SubTaskInfo("Processing documents for MeSH terms", 10);
    @Override
    public void process(ITaskListener listener) {
        ArrayList<String> files = FileUtils.getFiles(Constants.POS_DATA_FOLDER_BY_YEAR);
        int totalDocument = getTotalDocumentCount(files);
        final int[] processedCount = {0};

        final int[] meshCount = {0};
        final int[] happensInDocument = {0};
        final int[] meshTermLength = {0};
        Hashtable<String, MeSHOccurrence> occurrences = new Hashtable<>();

        for (String file : files) {
            Document.iterateOnDocumentListFile(file, new IDocumentLoaderCallback() {
                @Override
                public void processDocument(Document document, int i, int totalDocumentCount) {
                    processedCount[0]++;
                    if (processedCount[0] % 1000 == 0)
                        updateProgress(listener, PROCESSING_TASK, processedCount[0], totalDocument);

                    meshCount[0] += document.categories.size();
                    for (String mesh : document.categories) {
                        meshTermLength[0] += mesh.split(" ").length + 1;

                        MeSHOccurrence o = null;
                        if (occurrences.containsKey(mesh)) {
                            o = occurrences.get(mesh);
                        } else {
                            o = new MeSHOccurrence();
                            occurrences.put(mesh, o);
                            o.mesh = mesh;
                        }

                        o.totalCount++;
                        if (hasMeSHTerms(mesh.toLowerCase(), document)) {
                            happensInDocument[0]++;
                            o.happensInDocument++;
                        }
                    }
                }
            });
        }

        MeSHOccurrence[] list = new MeSHOccurrence[occurrences.size()];
        int index = 0;
        for (String mesh : occurrences.keySet()) {
            list[index] = occurrences.get(mesh);
            index++;
        }

        Arrays.sort(list);
        StringBuilder result = new StringBuilder();
        StringBuilder result2 = new StringBuilder();
        int i = 0;
        for (MeSHOccurrence m : list) {
            result.append(String.format("%s: %.3f\r\n", m.mesh, m.getProbability()));
            result2.append(String.format("%d,%.3f\r\n", i, m.getProbability()));
            i++;
        }

        FileUtils.writeText(Constants.TEMP_FOLDER + "mesh-occurrent-classifier-probability.txt", result.toString());
        FileUtils.writeText(Constants.TEMP_FOLDER + "mesh-occurrent-classifier-probability.csv", result2.toString());

        listener.log("Total MeSH Labels: " + meshCount[0]);
        listener.log("MeSH Labels Occurrence in Documents: " + happensInDocument[0]);
        listener.log(String.format("Probability of MeSH Labels Occurrence in Documents: %.2f", happensInDocument[0]*100.0/meshCount[0]));
        listener.log("Average MeSH Terms Length: " + meshTermLength[0] * 1.0 / meshCount[0]);
    }

    private boolean hasMeSHTerms(String mesh, Document document) {
        String[] parts = mesh.replace(",", " ").split(" ");
        int count = 0;

        for (String token : parts) {
            boolean found = false;
            for (int i = 0; i < document.text.size() && !found; i++) {
                for (int j = 0; j < document.text.get(i).tokens.size() && !found; j++) {
                    if (document.text.get(i).tokens.get(j).token.toLowerCase().equals(token)) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                for (int j = 0; j < document.title.tokens.size() && !found; j++) {
                    if (document.title.tokens.get(j).token.toLowerCase().equals(token)) {
                        found = true;
                        break;
                    }
                }
            }

            if (found)
                count++;
        }

        int nu = 0;
        if (parts.length > 2){
            nu = 1;
        }

        return count + nu >= parts.length;
    }

    private int getTotalDocumentCount(ArrayList<String> files) {
        int result = 0;

        for (String file : files) {
            result += Document.getDocumentCountInFile(file);
        }

        return result;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_EXPERIMENT;
    }

    @Override
    protected String getTaskTitle() {
        return "MeSH Class Term Occurrence in Document";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result= new ArrayList<>();

        result.add(PROCESSING_TASK);
        return result;
    }
}

class MeSHOccurrence implements Comparable<MeSHOccurrence> {
    public String mesh = "";
    public int totalCount = 0;
    public int happensInDocument = 0;

    public double getProbability() {
        return happensInDocument*1.0 / totalCount;
    }

    @Override
    public int compareTo(MeSHOccurrence o) {
        return new Double(o.getProbability()).compareTo(getProbability());
    }
}
