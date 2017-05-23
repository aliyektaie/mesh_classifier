package edu.goergetown.bioasq.core.feature_extractor;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.*;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.DocumentListUtils;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by yektaie on 5/18/17.
 */
public abstract class BaseTermFrequencyFeatureExtractor implements IDocumentFeatureExtractor {
    private static boolean preprocessDone = false;
    private static int documentCount = 0;
    private static int documentAverageLength;
    private static Hashtable<String, Integer> numberOfDocumentForEachTerm = null;

    @Override
    public String getDestinationFolder() {
        String path = Constants.DOCUMENT_FEATURES_DATA_FOLDER;
        path += (getWeightFolderName() + Constants.BACK_SLASH);
        return path;
    }

    protected abstract String getWeightFolderName();

    @Override
    public boolean needNormalizationOfFeatures() {
        return false;
    }

    @Override
    public void normalizeFeatures(ITaskListener listener, ArrayList<DocumentFeatureSet> documentsFeatureList, ArrayList<Document> documents) {

    }

    @Override
    public boolean needPreprocessTask() {
        return true;
    }

    @Override
    public int getPreprocessTaskGetTimeInMinutes() {
        return 5;
    }

    @Override
    public void prepreocess(ITaskListener listener) {
        if (preprocessDone) {
            return;
        }

        ArrayList<Integer> years = DocumentListUtils.getAvailableDocumentYears();
        final int[] documentCount = {0};
        final int[] documentAverageLength = {0};
        Hashtable<String, Integer> numberOfDocumentForEachTerm = new Hashtable<>();

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            for (int year : years) {
                String path = Constants.POS_DATA_FOLDER_BY_YEAR + year + "-" + i + ".bin";
                listener.log("");
                listener.log("Processing file " + year + "-" + i + ".bin");

                final int[] docCountProcessed = {0};
                Document.iterateOnDocumentListFile(path, new IDocumentLoaderCallback() {
                    @Override
                    public void processDocument(Document document, int i, int totalDocumentCount) {
                        documentCount[0]++;
                        docCountProcessed[0]++;

                        if (docCountProcessed[0] % 10000 == 0) {
                            listener.log("   " + docCountProcessed[0] + " of " + totalDocumentCount);
                        }
                        Hashtable<String, Integer> tokens = exctractBagOfWords(document);
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
        }

        BaseTermFrequencyFeatureExtractor.documentCount = documentCount[0];
        BaseTermFrequencyFeatureExtractor.documentAverageLength = documentAverageLength[0] / BaseTermFrequencyFeatureExtractor.documentCount;
        BaseTermFrequencyFeatureExtractor.numberOfDocumentForEachTerm = numberOfDocumentForEachTerm;

        preprocessDone = true;
    }

    @Override
    public DocumentFeatureSet extractFeatures(Document document) {
        Hashtable<String, Integer> bagOfWords = exctractBagOfWords(document);
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

    private Hashtable<String, Integer> exctractBagOfWords(Document document) {
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
