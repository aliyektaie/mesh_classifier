package edu.goergetown.bioasq.core.feature_extractor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.Sentence;
import edu.goergetown.bioasq.core.document.Term;
import edu.goergetown.bioasq.core.feature_extractor.IDocumentFeatureExtractor;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.Matrix;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by yektaie on 5/15/17.
 */
public class TopicRankFeatureExtractor implements IDocumentFeatureExtractor {
    public static final double NOUN_PHRASE_SIMILARITY_THRESHOLD = 33.32;
    private static ArrayList<String> NOUN_PHRASE_TAGS = null;

    private static final double LANDA = 0.85;
    private static final int SCORE_CALCULATION_ITERATION_COUNT = 20;

    static {
        NOUN_PHRASE_TAGS = new ArrayList<>();

        NOUN_PHRASE_TAGS.add("JJ");
        NOUN_PHRASE_TAGS.add("NN");
        NOUN_PHRASE_TAGS.add("JJR");
        NOUN_PHRASE_TAGS.add("NNP");
        NOUN_PHRASE_TAGS.add("FW");
        NOUN_PHRASE_TAGS.add("JJS");
        NOUN_PHRASE_TAGS.add("NNS");
        NOUN_PHRASE_TAGS.add("NNPS");
    }

    @Override
    public String getDestinationFolder() {
        String path = Constants.DOCUMENT_FEATURES_DATA_FOLDER;
        path += ("topic-rank" + Constants.BACK_SLASH);
        return path;
    }

    @Override
    public String getTitle() {
        return "TopicRank Keyword Extractor";
    }

    @Override
    public boolean needNormalizationOfFeatures() {
        return false;
    }

    @Override
    public DocumentFeatureSet extractFeatures(Document document) {
        DocumentFeatureSet result = new DocumentFeatureSet(getTitle());
        ArrayList<NounPhrase> terms = getNounPhrases(document);
        Matrix graph = createWeightsGraph(terms);

        double[] scores = calculateTopicScores(terms, graph);
        buildResultFeatureSet(result, terms, scores);

        return result;
    }

    public static void buildResultFeatureSet(DocumentFeatureSet result, ArrayList<NounPhrase> terms, double[] scores) {
        for (int i = 0; i < terms.size(); i++) {
            for (int j = 0; j < terms.get(i).phrase.size(); j++) {
                result.addFeature(terms.get(i).phrase.get(j), scores[i]);
            }
        }
    }

    public static double[] calculateTopicScores(ArrayList<NounPhrase> terms, Matrix graph) {
        double[] result = initializeScores(terms);
        double[] temp = new double[terms.size()];

        for (int iteration = 0; iteration < SCORE_CALCULATION_ITERATION_COUNT; iteration++) {
            for (int i = 0; i < terms.size(); i++) {
                double contribution = 0;
                for (int j = 0; j < terms.size(); j++) {
                    contribution += ((graph.get(i,j) * result[j]) / graph.sumColumn(j));
                }

                temp[i] = (1 - LANDA) + LANDA * contribution;
            }

            double[] t = temp;
            temp = result;
            result = t;
        }

        return result;
    }

    private static double[] initializeScores(ArrayList<NounPhrase> terms) {
        double[] result = new double[terms.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = 1.0;
        }
        return result;
    }

    public static Matrix createWeightsGraph(ArrayList<NounPhrase> terms) {
        Matrix result = new Matrix(terms.size(), terms.size());

        for (int row = 0; row < terms.size(); row++) {
            for (int column = 0; column < row; column++) {
                result.set(row, column, calculateWeight(terms.get(row), terms.get(column)), true);
            }
        }

        return result;
    }

    private static double calculateWeight(NounPhrase ti, NounPhrase tj) {
        double result = 0;

        for (int i = 0; i < ti.phrase.size(); i++) {
            for (int j = 0; j < tj.phrase.size(); j++) {
                for (int ii = 0; ii < ti.locations.get(i).size(); ii++) {
                    for (int jj = 0; jj < tj.locations.get(j).size(); jj++) {
                        double diff = Math.abs(ti.locations.get(i).get(ii) - tj.locations.get(j).get(jj));
                        if (diff != 0) {
                            result += (1.0 / diff);
                        }
                    }
                }
            }
        }

        return result;
    }

    private ArrayList<NounPhrase> getNounPhrases(Document document) {
        ArrayList<NounPhrase> result = new ArrayList<>();

        int beginIndex = 0;
        beginIndex += addNounPhrases(result, document.title, beginIndex);

        for (Sentence sentence : document.text) {
            beginIndex += addNounPhrases(result, sentence, beginIndex);
        }

        return mergeSimilarNounPhrase(result);
    }

    private ArrayList<NounPhrase> mergeSimilarNounPhrase(ArrayList<NounPhrase> list) {
        ArrayList<NounPhrase> result = new ArrayList<>();

        for (NounPhrase nounPhrase : list) {
            int index = findSimilarNounPhrase(result, nounPhrase);
            if (index == -1) {
                result.add(nounPhrase);
            } else if (index >= 0) {
                result.get(index).phrase.add(nounPhrase.phrase.get(0));
                result.get(index).locations.add(nounPhrase.locations.get(0));
            }
        }

        return result;
    }

    private int findSimilarNounPhrase(ArrayList<NounPhrase> list, NounPhrase nounPhrase) {
        int result = -1;

        int index = 0;

        for (NounPhrase np : list) {
            for (int i = 0; i < np.phrase.size(); i++) {
                double similarity = getSimilarity(np.phrase.get(i), nounPhrase.phrase.get(0));
                if (similarity == 100.0) {
                    return -2;
                } else if (similarity > NOUN_PHRASE_SIMILARITY_THRESHOLD) {
                    return index;
                }
            }

            index++;
        }

        return result;
    }

    public static double getSimilarity(String np1, String np2) {
        String[] np1Parts = np1.split(" ");
        String[] np2Parts = np2.split(" ");

        ArrayList<String> common = getCommonTerms(np1Parts, np2Parts);
        if (common.size() == np1Parts.length && common.size() == np2Parts.length) {
            return 100.0;
        } else {
            return 200.0 * common.size() / (np1Parts.length + np2Parts.length);
        }
    }

    private static ArrayList<String> getCommonTerms(String[] p1, String[] p2) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> p1List = new ArrayList<>(Arrays.asList(p1));

        for (int i = 0; i < p2.length; i++) {
            if (p1List.contains(p2[i])) {
                result.add(p2[i]);
            }
        }

        return result;
    }

    private int addNounPhrases(ArrayList<NounPhrase> result, Sentence sentence, int index) {
        if (sentence == null || sentence.tokens == null)
            return index;

        NounPhrase current = new NounPhrase();
        for (Term token : sentence.tokens) {
            if (NOUN_PHRASE_TAGS.contains(token.partOfSpeech)) {
                String stemmed = token.token.toLowerCase();
                current.phrase.set(0, (current.phrase.get(0) + " " + stemmed).trim());
                current.locations.get(0).add(index);
            } else {
                if (!current.phrase.get(0).equals("")) {
                    result.add(current);
                    current = new NounPhrase();
                }
            }

            index++;
        }

        if (!current.phrase.get(0).equals("")) {
            result.add(current);
        }

        return index;
    }

    @Override
    public void normalizeFeatures(ITaskListener listener, ArrayList<DocumentFeatureSet> documentsFeatureList, ArrayList<Document> documents) {

    }

    @Override
    public boolean needPreprocessTask() {
        return false;
    }

    @Override
    public int getPreprocessTaskGetTimeInMinutes() {
        return 0;
    }

    @Override
    public void prepreocess(ITaskListener listener) {

    }

    @Override
    public String toString() {
        return getTitle();
    }

    public void test() {
        Document d = new Document();
        d.title = parse("JJ Inverse  NNS problems  IN for  DT a  JJ mathematical  NN model  IN of  NN ion  NN exchange  IN in  DT a  JJ compressible  NN ion  RBR exchanger").get(0);
        d.text = parse("DT A  JJ mathematical  NN model  IN of  NN ion  NN exchange  VBZ is  VBN considered , ,  VBG allowing  IN for  NN ion  RBR exchanger  NN compression  IN in  DT the  NN process  IN of  NN ion  NN exchange  . .  CD Two  NN inverse  NNS problems  VBP are  VBN investigated  IN for  DT this  NN model , ,  JJ unique  NN solvability  VBZ is  VBN proved , ,  CC and  JJ numerical  NN solution  NNS methods  VBP are  VBN proposed  . .  DT The  NN efficiency  IN of  DT the  VBN proposed  NNS methods  VBZ is  VBN demonstrated  IN by  DT a  JJ numerical  NN experiment  . .");

        DocumentFeatureSet fSet = new TopicRankFeatureExtractor().extractFeatures(d);
        System.out.println(fSet.toString());
    }

    private static ArrayList<Sentence> parse(String s) {
        ArrayList<Sentence> result = new ArrayList<>();
        String[] parts = s.split("  ");

        Sentence sen = new Sentence();
        result.add(sen);

        for (String part : parts) {
            String[] p = part.split(" ");
            if (p[0].equals(".")) {
                sen = new Sentence();
                result.add(sen);
            } else {
                Term t = new Term();
                t.token = p[1];
                t.partOfSpeech = p[0];

                sen.tokens.add(t);
            }
        }

        return result;
    }
}
