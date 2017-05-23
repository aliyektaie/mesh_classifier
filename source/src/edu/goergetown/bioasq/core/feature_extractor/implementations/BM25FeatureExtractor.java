package edu.goergetown.bioasq.core.feature_extractor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.*;
import edu.goergetown.bioasq.core.feature_extractor.BaseTermFrequencyFeatureExtractor;
import edu.goergetown.bioasq.core.feature_extractor.IDocumentFeatureExtractor;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.DocumentListUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

/**
 * Created by Yektaie on 5/18/2017.
 */
public class BM25FeatureExtractor extends BaseTermFrequencyFeatureExtractor {
    private static ArrayList<String> toBeIgnoredPOS = new ArrayList<>();

    private static final double K = 1.5;
    private static final double B = 0.75;

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
        return "BM25F Term Weighting";
    }


    @Override
    protected String getWeightFolderName() {
        return "bm25f";
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
        double weight = (termFrequency * (K + 1)) / (termFrequency + K * (1 - B + B * (documentLength * 1.0 / documentAverageLength)));
        double idf = Math.log10((documentCount - documentCountContainingTerm + 0.5) / (documentCountContainingTerm + 0.5));
        return weight * idf;
    }

}
