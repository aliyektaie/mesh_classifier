package edu.goergetown.bioasq.core.feature_extractor.implementations;

import edu.goergetown.bioasq.core.document.Term;
import edu.goergetown.bioasq.core.feature_extractor.BaseTermFrequencyFeatureExtractor;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/18/17.
 */
public class TFIDFFeatureExtractor extends BaseTermFrequencyFeatureExtractor {
    private static ArrayList<String> toBeIgnoredPOS = new ArrayList<>();

    private static final int TITLE_COEFFICIENT = 4;
    private static final int TEXT_COEFFICIENT = 1;

    static {
        toBeIgnoredPOS.add(":");
        toBeIgnoredPOS.add("IN");
        toBeIgnoredPOS.add("CC");
        toBeIgnoredPOS.add("CD");
        toBeIgnoredPOS.add(".");
        toBeIgnoredPOS.add(",");
        toBeIgnoredPOS.add("DT");
        toBeIgnoredPOS.add("WDT");
        toBeIgnoredPOS.add("TO");
        toBeIgnoredPOS.add("``");
        toBeIgnoredPOS.add("''");
        toBeIgnoredPOS.add("WP");
        toBeIgnoredPOS.add("MD");
        toBeIgnoredPOS.add("PRP");
        toBeIgnoredPOS.add("POS");
        toBeIgnoredPOS.add("PRP$");
        toBeIgnoredPOS.add("PDT");
        toBeIgnoredPOS.add("EX");
        toBeIgnoredPOS.add("WP$");
        toBeIgnoredPOS.add("$");
        toBeIgnoredPOS.add("UH");
        toBeIgnoredPOS.add("#");
    }

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
        return !toBeIgnoredPOS.contains(token.partOfSpeech);
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
