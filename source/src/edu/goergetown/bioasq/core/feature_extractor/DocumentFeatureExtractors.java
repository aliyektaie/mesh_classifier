package edu.goergetown.bioasq.core.feature_extractor;

import edu.goergetown.bioasq.core.feature_extractor.implementations.BM25FeatureExtractor;
import edu.goergetown.bioasq.core.feature_extractor.implementations.TFIDFFeatureExtractor;
import edu.goergetown.bioasq.core.feature_extractor.implementations.UMLSFeatureExtractor;
import edu.goergetown.bioasq.core.feature_extractor.implementations.TopicRankFeatureExtractor;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/15/17.
 */
public class DocumentFeatureExtractors {
    private static ArrayList<IDocumentFeatureExtractor> featureExtractors = null;

    public static ArrayList<IDocumentFeatureExtractor> getFeatureExtractors() {
        if (featureExtractors == null) {
            featureExtractors = new ArrayList<>();

            featureExtractors.add(new BM25FeatureExtractor());
            featureExtractors.add(new TFIDFFeatureExtractor());
            featureExtractors.add(new TopicRankFeatureExtractor());
            featureExtractors.add(new UMLSFeatureExtractor());
        }

        return featureExtractors;
    }
}
