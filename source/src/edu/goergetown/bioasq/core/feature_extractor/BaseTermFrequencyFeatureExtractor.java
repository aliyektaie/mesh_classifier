package edu.goergetown.bioasq.core.feature_extractor;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.*;
import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by yektaie on 5/18/17.
 */
public abstract class BaseTermFrequencyFeatureExtractor implements IDocumentFeatureExtractor {
    private boolean preprocessDone = false;
    private int documentCount = 0;
    private int documentAverageLength;
    private Hashtable<String, Integer> numberOfDocumentForEachTerm = null;
    private ArrayList<String> inputFiles = null;

    @Override
    public int getCoreCount() {
        return Constants.CORE_COUNT;
    }

    @Override
    public void setInputFiles(ArrayList<String> inputFiles) {
        this.inputFiles = inputFiles;
    }

    @Override
    public String getDestinationFolderName() {
        return getWeightFolderName();
    }

    protected abstract String getWeightFolderName();

    @Override
    public boolean needPreprocessTask() {
        return true;
    }

    @Override
    public int getPreprocessTaskGetTimeInMinutes() {
        return 5;
    }

    @Override
    public void preprocess(ITaskListener listener) {
        if (preprocessDone) {
            return;
        }

        final int[] documentCount = {0};
        final int[] documentAverageLength = {0};
        Hashtable<String, Integer> numberOfDocumentForEachTerm = new Hashtable<>();

        for (String path : inputFiles) {

            listener.log("");
            listener.log("Processing file " + path.substring(path.lastIndexOf(Constants.BACK_SLASH) + 1));

            final int[] docCountProcessed = {0};
            Document.iterateOnDocumentListFile(path, new IDocumentLoaderCallback() {
                @Override
                public void processDocument(Document document, int i, int totalDocumentCount) {
                    documentCount[0]++;
                    docCountProcessed[0]++;

                    if (docCountProcessed[0] % 10000 == 0) {
                        listener.log("   " + docCountProcessed[0] + " of " + totalDocumentCount);
                    }
                    Hashtable<String, Integer> tokens = extractBagOfWords(document);
                    for (String term : tokens.keySet()) {
                        documentAverageLength[0] += tokens.get(term);

                        int numberOfDocumentWithTerm = 1;
                        if (numberOfDocumentForEachTerm.containsKey(term)) {
                            numberOfDocumentWithTerm += numberOfDocumentForEachTerm.get(term);
                        }

                        numberOfDocumentForEachTerm.put(term, numberOfDocumentWithTerm);
                    }
                }
            });
        }

        this.documentCount = documentCount[0];
        this.documentAverageLength = documentAverageLength[0] / this.documentCount;
        this.numberOfDocumentForEachTerm = numberOfDocumentForEachTerm;

        preprocessDone = true;
    }

    @Override
    public DocumentFeatureSet extractFeatures(Document document) {
        Hashtable<String, Integer> bagOfWords = extractBagOfWords(document);
        int documentLength = getDocumentLength(bagOfWords);

        DocumentFeatureSet result = new DocumentFeatureSet(getTitle());

        for (String term : bagOfWords.keySet()) {
            int freq = bagOfWords.get(term);
            int gFreq = numberOfDocumentForEachTerm.get(term);

            double score = getWeightForTerm(freq, gFreq, documentLength, documentAverageLength, documentCount);

            result.addFeature(term, score);
        }

        return result;
    }

    private int getDocumentLength(Hashtable<String, Integer> bagOfWords) {
        int result = 0;

        for (String term : bagOfWords.keySet()) {
            result += bagOfWords.get(term);
        }

        return result;
    }

    private Hashtable<String, Integer> extractBagOfWords(Document document) {
        Hashtable<String, Integer> result = new Hashtable<>();

        addTerms(result, document.title, getWeightForTitleTokens());
        for (Sentence sentence : document.text) {
            addTerms(result, sentence, getWeightForAbstractTokens());
        }

        return result;
    }

    private void addTerms(Hashtable<String, Integer> result, Sentence sentence, int coefficient) {
        for (Term term : sentence.tokens) {
            if (!shouldConsiderToken(term)) {
                continue;
            }

            String stemmedTerm = term.getStemmed();
            int count = coefficient;

            if (result.containsKey(stemmedTerm)) {
                count += result.get(stemmedTerm);
            }

            result.put(stemmedTerm, count);
        }
    }

    protected abstract boolean shouldConsiderToken(Term token);

    protected abstract int getWeightForTitleTokens();

    protected abstract int getWeightForAbstractTokens();

    protected abstract double getWeightForTerm(int termFrequency, int documentCountContainingTerm, int documentLength, int documentAverageLength, int documentCount);

    @Override
    public String toString() {
        return getTitle();
    }
}
