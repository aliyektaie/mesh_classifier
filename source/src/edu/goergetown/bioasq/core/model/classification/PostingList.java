package edu.goergetown.bioasq.core.model.classification;

import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.FeatureValuePair;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Created by yektaie on 5/29/17.
 */
public class PostingList {
    private Hashtable<String, ArrayList<PostingListEntry>> entries = new Hashtable<>();
    private String[] vocabulary = null;

    public static PostingList createFrom(ITaskListener listener, ArrayList<Vector> documentsFeatures) {
        PostingList result = new PostingList();
        listener.setCurrentState("Creating posting list");
        int showProgressCount = documentsFeatures.size() / 1000;
        ArrayList<String> vocabulary = new ArrayList<>();

        for (int i = 0; i < documentsFeatures.size(); i++) {
            if (i % showProgressCount == 0)
                listener.setProgress(i, documentsFeatures.size());

            Vector featureSet = documentsFeatures.get(i);
            for (int j = 0; j < featureSet.featureCount(); j++) {
                FeatureValuePair feature = featureSet.getFeature(j);
                vocabulary.addAll(result.addTerm(featureSet.identifier, feature));
            }
        }

        listener.setCurrentState("Sorting posting list vocabulary");
        result.vocabulary = sortList(vocabulary);

        return result;
    }

    private static String[] sortList(ArrayList<String> vocabulary) {
        String[] result = new String[vocabulary.size()];

        for (int i = 0; i < vocabulary.size(); i++) {
            result[i] = vocabulary.get(i);
        }

        Arrays.sort(result);

        return result;
    }

    private ArrayList<String> addTerm(String identifier, FeatureValuePair feature) {
        ArrayList<String> result = new ArrayList<>();

        String[] parts = feature.name.split(";");

        for (int i = 0; i < parts.length; i++) {
            PostingListEntry entry = new PostingListEntry();
            entry.identifier = identifier;
            entry.term = parts[i].trim();
            entry.value = feature.value;

            if (!entries.containsKey(entry.term)) {
                entries.put(entry.term, new ArrayList<>());
                result.add(entry.term);
            }

            entries.get(entry.term).add(entry);
        }

        return result;
    }
}
