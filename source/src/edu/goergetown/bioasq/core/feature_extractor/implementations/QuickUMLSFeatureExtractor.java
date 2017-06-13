package edu.goergetown.bioasq.core.feature_extractor.implementations;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.IDocumentLoaderCallback;
import edu.goergetown.bioasq.core.feature_extractor.IDocumentFeatureExtractor;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by yektaie on 6/11/17.
 */
public class QuickUMLSFeatureExtractor implements IDocumentFeatureExtractor {
    private static final String QUICK_UMLS_TEMP_FOLDER_NAME = "QuickUMLS Temp";
    private static final String TRAIN = "train";
    private static final String DEV = "dev";
    private static final String TEST = "test";
    private boolean preprocessDone = false;
    private static int documentCount = 0;
    private static int documentAverageLength;
    private static Hashtable<String, Integer> numberOfDocumentForEachTerm = null;
    private Hashtable<String, ArrayList<StringIntPair>> documents = null;
    private ArrayList<String> inputFiles = null;

    private String sourceType = "";

    private static final double K = 1.5;
    private static final double B = 0.75;

    @Override
    public void setInputFiles(ArrayList<String> inputFiles) {
        this.inputFiles = inputFiles;
    }

    @Override
    public int getCoreCount() {
        return Constants.CORE_COUNT;
    }

    @Override
    public String getDestinationFolderName() {
        return "quick-umls";
    }

    @Override
    public String getTitle() {
        return "QuickUMLS";
    }

    @Override
    public boolean needPreprocessTask() {
        return true;
    }

    @Override
    public int getPreprocessTaskGetTimeInMinutes() {
        return 10;
    }

    @Override
    public void preprocess(ITaskListener listener) {
        setInputType();
        if (FileUtils.exists(getCompleteFrequencyFilePath())) {
            preprocessFrequencyFile(listener);
        } else {
            mergeTemporaryFiles(listener);
            createFinalFrequencyFile(listener);
            listener.log("Frequency file created");
        }
    }

    private void setInputType() {
        String path = inputFiles.get(0);
        if (path.startsWith(Constants.POS_DATA_FOLDER_BY_YEAR)) {
            this.sourceType = TRAIN;
        } else if (path.contains("dev_set.bin")) {
            this.sourceType = DEV;
        } else if (path.contains("test_set.bin")) {
            this.sourceType = TEST;
        }
    }

    private void preprocessFrequencyFile(ITaskListener listener) {
        if (preprocessDone) {
            return;
        }

        int documentCount = 0;
        int documentAverageLength = 0;

        Hashtable<String, Integer> numberOfDocumentForEachTerm = new Hashtable<>();

        listener.log("Loading file: " + getCompleteFrequencyFilePath());
        this.documents = loadFinalFrequencyFile();
        int docCountProcessed = 0;
        int totalDocumentCount = this.documents.size();

        for (String identifier : this.documents.keySet()) {
            documentCount++;
            docCountProcessed++;

            if (docCountProcessed % 10000 == 0) {
                listener.log("   " + docCountProcessed + " of " + totalDocumentCount);
            }

            Hashtable<String, Integer> tokens = extractBagOfWords(this.documents.get(identifier));
            for (String term : tokens.keySet()) {
                documentAverageLength += tokens.get(term);

                int numberOfDocumentWithTerm = 1;
                if (numberOfDocumentForEachTerm.containsKey(term)) {
                    numberOfDocumentWithTerm += numberOfDocumentForEachTerm.get(term);
                }

                numberOfDocumentForEachTerm.put(term, numberOfDocumentWithTerm);
            }
        }

        this.documentCount = documentCount;
        this.documentAverageLength = documentAverageLength / this.documentCount;
        this.numberOfDocumentForEachTerm = numberOfDocumentForEachTerm;

        preprocessDone = true;
    }

    private Hashtable<String, Integer> extractBagOfWords(ArrayList<StringIntPair> list) {
        Hashtable<String, Integer> result = new Hashtable<>();

        for (StringIntPair p : list) {
            result.put(p.term, p.count);
        }

        return result;
    }

    private Hashtable<String, ArrayList<StringIntPair>> loadFinalFrequencyFile() {
        Hashtable<String, ArrayList<StringIntPair>> result = new Hashtable<>();

        String[] lines = FileUtils.readAllLines(getCompleteFrequencyFilePath());
        String id = "";

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("pmid")) {
                id = lines[i].trim();
                result.put(id, new ArrayList<>());
            } else if (lines[i].startsWith("\t")) {
                String[] parts = lines[i].trim().split(" -> ");
                StringIntPair pair = new StringIntPair();
                pair.count = Integer.valueOf(parts[1]);
                pair.term = parts[0].toLowerCase();

                result.get(id).add(pair);
            }
        }

        return result;
    }

    private void createFinalFrequencyFile(ITaskListener listener) {
        Hashtable<String, ArrayList<StringIntPair>> frequencies = loadAllFrequenciesFromResultFiles(listener);
        saveFinalFrequencyFile(frequencies, getCompleteFrequencyFilePath());
    }

    @NotNull
    private Hashtable<String, ArrayList<StringIntPair>> loadAllFrequenciesFromResultFiles(ITaskListener listener) {
        ArrayList<String> resultFiles = FileUtils.getFiles(getResultsTempFolder());
        Hashtable<String, ArrayList<StringIntPair>> frequencies = new Hashtable<>();

        String id = "";
        listener.log("Processing temporary result files");
        for (String filePath : resultFiles) {
            listener.log("    " + filePath);
            String[] lines = FileUtils.readAllLines(filePath);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith("pmid")) {
                    id = lines[i].trim();
                    frequencies.put(id, new ArrayList<>());
                } else if (lines[i].startsWith("\t")) {
                    String[] parts = lines[i].trim().split(" -> ");
                    StringIntPair pair = new StringIntPair();
                    pair.count = Integer.valueOf(parts[1]);
                    pair.term = parts[0];

                    frequencies.get(id).add(pair);
                }
            }
        }
        return frequencies;
    }

    private void mergeTemporaryFiles(ITaskListener listener) {
        String folder = Constants.TEMP_FOLDER + QUICK_UMLS_TEMP_FOLDER_NAME + Constants.BACK_SLASH;
        String folderResults = getResultsTempFolder();

        FileUtils.createDirectory(folderResults);

        for (int i = 0; i < 4; i++) {
            String inputs = folder + "input-" + i + ".txt";
            String results = folder + "results-" + i + ".txt";
            if (FileUtils.exists(results) && FileUtils.exists(inputs)) {
                listener.log("processing '" + inputs + "'");
                Hashtable<String, ArrayList<String>> content = loadInputs(inputs);
                listener.log("   input loaded");
                Hashtable<String, ArrayList<String>> parsed = loadInputs(results);
                listener.log("   parsed loaded");

                Hashtable<String, ArrayList<String>> done = new Hashtable<>();
                Hashtable<String, ArrayList<String>> remained = new Hashtable<>();

                processDoneAndRemains(done, remained, content, parsed);
                listener.log("   done and remained loaded");
                String path = folderResults + "result-" + FileUtils.getFiles(folderResults).size() + ".txt";
                saveDones(done, path);
                listener.log("   done saved");
                saveInputs(remained, inputs);
            }
        }
    }

    @NotNull
    private String getResultsTempFolder() {
        return Constants.TEMP_FOLDER + QUICK_UMLS_TEMP_FOLDER_NAME + Constants.BACK_SLASH + "Results-" + sourceType + Constants.BACK_SLASH;
    }

    private String getCompleteFrequencyFilePath() {
        return Constants.TEMP_FOLDER + QUICK_UMLS_TEMP_FOLDER_NAME + Constants.BACK_SLASH + "quick-umls-" + sourceType + "-frequencies.txt";
    }

    private void saveInputs(Hashtable<String, ArrayList<String>> remained, String path) {
        StringBuilder result = new StringBuilder();

        for (String key : remained.keySet()) {
            result.append(key);
            result.append("\n");

            ArrayList<String> lines = remained.get(key);
            for (String line : lines) {
                result.append(line);
                result.append("\n");
            }

            result.append("\n");
        }

        FileUtils.writeText(path, result.toString());
    }

    private void saveDones(Hashtable<String, ArrayList<String>> done, String path) {
        StringBuilder result = new StringBuilder();

        for (String key : done.keySet()) {
            result.append(key);
            result.append("\n");

            ArrayList<String> lines = getTerms(done.get(key));
            for (String line : lines) {
                result.append("\t");
                result.append(line);
                result.append("\n");
            }

            result.append("\n");
        }

        FileUtils.writeText(path, result.toString());
    }

    private void saveFinalFrequencyFile(Hashtable<String, ArrayList<StringIntPair>> done, String path) {
        StringBuilder result = new StringBuilder();

        for (String key : done.keySet()) {
            result.append(key);
            result.append("\r\n");

            ArrayList<StringIntPair> lines = done.get(key);
            for (StringIntPair term : lines) {
                result.append("\t");
                result.append(term.term);
                result.append(" -> " + term.count);
                result.append("\r\n");
            }

            result.append("\r\n");
        }

        FileUtils.writeText(path, result.toString());
    }

    private ArrayList<String> getTerms(ArrayList<String> strings) {
        Hashtable<String, Integer> counts = new Hashtable<>();
        ArrayList<String> result = new ArrayList<>();

        for (String sentence : strings) {
            String[] parts = sentence.split("u'term': u'");
            for (int i = 1; i < parts.length; i++) {
                String term = parts[i];
                term = term.substring(0, term.indexOf("'"));

                int count = 0;
                if (counts.containsKey(term)) {
                    count = counts.get(term);
                }

                counts.put(term, count + 1);
            }
        }

        for (String term : counts.keySet()) {
            int count = counts.get(term);

            result.add(term + " -> " + count);
        }

        return result;
    }

    private void processDoneAndRemains(Hashtable<String, ArrayList<String>> done, Hashtable<String, ArrayList<String>> remained, Hashtable<String, ArrayList<String>> content, Hashtable<String, ArrayList<String>> parsed) {
        for (String key : content.keySet()) {
            if (parsed.containsKey(key)) {
                ArrayList<String> contentLines = content.get(key);
                ArrayList<String> parsedLines = parsed.get(key);

                if (contentLines.size() == parsedLines.size()) {
                    done.put(key, parsedLines);
                } else {
                    remained.put(key, contentLines);
                }
            } else {
                remained.put(key, content.get(key));
            }
        }
    }

    private Hashtable<String, ArrayList<String>> loadInputs(String inputs) {
        Hashtable<String, ArrayList<String>> result = new Hashtable<>();

        String[] lines = FileUtils.readAllLines(inputs);
        String id = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.startsWith("pmid")) {
                result.put(line, new ArrayList<>());
                id = line;
            } else if (!line.trim().equals("")) {
                result.get(id).add(line);
            }
        }

        return result;
    }

    @Override
    public DocumentFeatureSet extractFeatures(Document document) {
        DocumentFeatureSet result = new DocumentFeatureSet(getTitle());

        String id = String.format("pmid: %s, year: %s", document.identifier, document.metadata.get("year").toString());
        Hashtable<String, Integer> bagOfWords = extractBagOfWords(this.documents.get(id));
        int documentLength = getDocumentLength(bagOfWords);


        for (String term : bagOfWords.keySet()) {
            int freq = bagOfWords.get(term);
            int gFreq = numberOfDocumentForEachTerm.get(term);

            double score = getWeightForTerm(freq, gFreq, documentLength, documentAverageLength, documentCount);

            result.addFeature(term, score);
        }

        return result;
    }

    private double getWeightForTerm(int termFrequency, int documentCountContainingTerm, int documentLength, int documentAverageLength, int documentCount) {
        double weight = (termFrequency * (K + 1)) / (termFrequency + K * (1 - B + B * (documentLength * 1.0 / documentAverageLength)));
        double idf = Math.log10((documentCount - documentCountContainingTerm + 0.5) / (documentCountContainingTerm + 0.5));
        double value = weight * idf;
        return value;
    }

    private int getDocumentLength(Hashtable<String, Integer> bagOfWords) {
        int result = 0;

        for (String term : bagOfWords.keySet()) {
            result += bagOfWords.get(term);
        }

        return result;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}

class StringIntPair {
    public int count = 0;
    public String term = "";

    @Override
    public String toString() {
        return term + " -> " + count;
    }
}
