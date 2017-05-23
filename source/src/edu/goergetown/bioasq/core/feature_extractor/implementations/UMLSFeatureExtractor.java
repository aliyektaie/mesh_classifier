package edu.goergetown.bioasq.core.feature_extractor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.Sentence;
import edu.goergetown.bioasq.core.document.Term;
import edu.goergetown.bioasq.core.feature_extractor.IDocumentFeatureExtractor;
import edu.goergetown.bioasq.core.umls.UMLSDictionary;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;
import edu.goergetown.bioasq.utils.Matrix;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by yektaie on 5/17/17.
 */
public class UMLSFeatureExtractor implements IDocumentFeatureExtractor {
    private UMLSDictionary umls = null;
    private static ArrayList<String> toBeIgnoredPOS = new ArrayList<>();

    static {
        toBeIgnoredPOS.add(":");
        toBeIgnoredPOS.add("IN");
        toBeIgnoredPOS.add("CC");
        toBeIgnoredPOS.add("-LRB-");
        toBeIgnoredPOS.add("CD");
        toBeIgnoredPOS.add("-RRB-");
        toBeIgnoredPOS.add(".");
        toBeIgnoredPOS.add(",");
        toBeIgnoredPOS.add("VBP");
        toBeIgnoredPOS.add("DT");
        toBeIgnoredPOS.add("WDT");
        toBeIgnoredPOS.add("VBN");
        toBeIgnoredPOS.add("TO");
        toBeIgnoredPOS.add("VBZ");
        toBeIgnoredPOS.add("VB");
        toBeIgnoredPOS.add("VBD");
        toBeIgnoredPOS.add("``");
        toBeIgnoredPOS.add("''");
        toBeIgnoredPOS.add("WP");
        toBeIgnoredPOS.add("WRB");
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
    public String getDestinationFolder() {
        String path = Constants.DOCUMENT_FEATURES_DATA_FOLDER;
        path += ("umls" + Constants.BACK_SLASH);
        return path;
    }

    @Override
    public String getTitle() {
        return "UMLS Term Extractor";
    }

    @Override
    public boolean needNormalizationOfFeatures() {
        return true;
    }

    @Override
    public DocumentFeatureSet extractFeatures(Document document) {
        DocumentFeatureSet result = new DocumentFeatureSet(getTitle());
        ArrayList<NounPhrase> terms = getBagOfWords(document);
        Matrix graph = TopicRankFeatureExtractor.createWeightsGraph(terms);

        double[] scores = TopicRankFeatureExtractor.calculateTopicScores(terms, graph);
        TopicRankFeatureExtractor.buildResultFeatureSet(result, terms, scores);

        return result;
    }

    private ArrayList<NounPhrase> getBagOfWords(Document document) {
        ArrayList<NounPhrase> result = new ArrayList<>();

        int tokenIndex= 0;
        tokenIndex = addTokens(result, document.title, tokenIndex);
        for (Sentence s : document.text) {
            tokenIndex += addTokens(result, s, tokenIndex);
        }

        return result;
    }

    private int addTokens(ArrayList<NounPhrase> result, Sentence sentence, int tokenIndex) {
        for (Term token : sentence.tokens) {
            tokenIndex++;
            if (toBeIgnoredPOS.contains(token.partOfSpeech)) {
                continue;
            }

            ArrayList<String> entries = umls.match(token.token);
            for (String entry : entries) {
                boolean added = false;
                for (NounPhrase np : result) {
                    for (int i = 0; i < np.phrase.size(); i++) {
                        if (TopicRankFeatureExtractor.getSimilarity(np.phrase.get(i),entry) > TopicRankFeatureExtractor.NOUN_PHRASE_SIMILARITY_THRESHOLD) {
                            boolean exists = false;
                            for (int j = 0; j < np.locations.get(i).size(); j++) {
                                exists = exists || (tokenIndex == np.locations.get(i).get(j));
                            }
                            if (!exists) {
                                np.locations.get(i).add(tokenIndex);
//                            } else {
//                                ArrayList<Integer> jj = np.locations.get(i);
//                                jj.toArray();
                            }
                            added = true;
                        }
                    }
                }

                if (!added) {
                    NounPhrase np = new NounPhrase();
                    np.phrase.set(0, entry);
                    np.locations.get(0).add(tokenIndex);
                    result.add(np);
                }
            }
        }

        return tokenIndex;
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
        return 1;
    }

    @Override
    public void prepreocess(ITaskListener listener) {
        this.umls = UMLSDictionary.loadDictionary(listener);
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
