package edu.goergetown.bioasq.core.feature_extractor.implementations;

import edu.goergetown.bioasq.core.document.Term;
import edu.goergetown.bioasq.core.feature_extractor.BaseTermFrequencyFeatureExtractor;
import edu.goergetown.bioasq.utils.PartOfSpeechUtils;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/18/17.
 */
public class TFIDFFeatureExtractor extends BaseTermFrequencyFeatureExtractor {
    private static ArrayList<String> toBeConsideredPOS = PartOfSpeechUtils.getPartOfSpeechToBeConsidered();

    private static final int TITLE_COEFFICIENT = 4;
    private static final int TEXT_COEFFICIENT = 1;

    @Override
    public String getTitle() {
        return "TF-IDF Term Weighting";
    }

    @Override
    protected String getWeightFolderName() {
        return "tf-idf";
    }

    @Override
    protected boolean shouldConsiderToken(Term token) {
        return toBeConsideredPOS.contains(token.partOfSpeech);
    }

    @Override
    protected int getWeightForTitleTokens() {
        return TITLE_COEFFICIENT;
    }

    @Override
    protected int getWeightForAbstractTokens() {
        return TEXT_COEFFICIENT;
    }

    @Override
    protected double getWeightForTerm(int termFrequency, int documentCountContainingTerm, int documentLength, int documentAverageLength, int documentCount) {
        return (termFrequency * 1.0 / documentLength) * Math.log10(documentCount * 1.0 / documentCountContainingTerm);
    }
}
