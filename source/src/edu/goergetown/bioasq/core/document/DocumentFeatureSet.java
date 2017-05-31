package edu.goergetown.bioasq.core.document;

import edu.goergetown.bioasq.core.model.*;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.*;
import java.util.*;

/**
 * Created by yektaie on 5/15/17.
 */
public class DocumentFeatureSet {
    public Hashtable<String, Double> features = new Hashtable<>();
    public String type = "";
    public String documentIdentifier = null;
    public int documentYear = 0;
    public ArrayList<String> meshList = new ArrayList<>();

    private DocumentFeatureSet() {

    }

    public  DocumentFeatureSet(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("DocumentFeatureSet [" + type + "]:");
        result.append("\n    Features [" + features.size() + "]:");

        KeyValuePair[] temp = new KeyValuePair[features.size()];
        int i = 0;
        for (String feature : features.keySet()) {
            temp[i] = new KeyValuePair(feature, features.get(feature));
            i++;
        }

        Arrays.sort(temp, new Comparator<KeyValuePair>() {
            @Override
            public int compare(KeyValuePair t0, KeyValuePair t1) {
                return new Double(t1.value).compareTo(t0.value);
            }
        });

        for (int j = 0; j < temp.length; j++) {
            result.append(String.format("\n        %s: %.5f", temp[j].key, temp[j].value));
        }

        result.append("\n    Labels [" + meshList.size() + "]:");
        for (int j = 0; j < temp.length; j++) {
            result.append(String.format("\n        [%d]: %s", j, meshList.get(j)));
        }

        return result.toString();
    }

    public void addFeature(String feature, double score) {
        features.put(feature, score);
    }

    public void load(byte[] array) {
        BinaryBuffer buffer = new BinaryBuffer(array);

        type = buffer.readString();
        documentIdentifier = buffer.readString();
        documentYear = buffer.readInt();

        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            String key = buffer.readString();
            double value = buffer.readFloat();

            features.put(key, value);
        }

        size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            String label = buffer.readString();
            meshList.add(label);
        }
    }

    public byte[] serialize() {
        BinaryBuffer buffer = new BinaryBuffer(getSerializedLength());

        buffer.append(type);
        buffer.append(documentIdentifier);
        buffer.append(documentYear);

        buffer.append(features.size());
        for (String feature : features.keySet()) {
            buffer.append(feature);
            double value = features.get(feature);
            buffer.append((float) value);
        }

        buffer.append(meshList.size());
        for (String label : meshList) {
            buffer.append(label);
        }

        return buffer.getBuffer();
    }

    public int getSerializedLength() {
        int result = 0;

        result += BinaryBuffer.getSerializedLength(type);
        result += BinaryBuffer.getSerializedLength(documentIdentifier);
        result += 4;
        result += 4;

        for (String key : features.keySet()) {
            result += BinaryBuffer.getSerializedLength(key);
            result += 4;
        }

        result += 4;
        for (String label : meshList) {
            result += BinaryBuffer.getSerializedLength(label);
        }

        return result;
    }

    public static void saveList(String path, ArrayList<DocumentFeatureSet> featuresList) {
        File file = new File(path);
        try {
            FileOutputStream fs = new FileOutputStream(file);
            writeInt(fs, featuresList.size());

            for (DocumentFeatureSet featureSet : featuresList) {
                byte[] data = featureSet.serialize();

                writeInt(fs, data.length);
                fs.write(data);
            }

            fs.flush();
            fs.close();
        } catch (Exception e) {
//            Logger.error(e, "Unable to write file at path \"%s\".", path);
        }
    }

    private static void writeInt(FileOutputStream fs, int size) throws IOException {
        BinaryBuffer buffer = new BinaryBuffer(4);
        buffer.append(size);

        fs.write(buffer.getBuffer());
    }

    public static ArrayList<DocumentFeatureSet> loadListFromFile(String path) {
        byte[] data = FileUtils.readBinaryFile(path);
        BinaryBuffer buffer = new BinaryBuffer(data);
        ArrayList<DocumentFeatureSet> result = new ArrayList<>();

        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            try {
                DocumentFeatureSet doc = new DocumentFeatureSet();
                doc.load(buffer.readByteArray());

                result.add(doc);
            } catch (Exception ex) {

            }
        }

        return result;
    }

    public static int getDocumentFeatureSetCountInFile(String path) {
        int result = 0;

        try {
            FileInputStream inputFile = new FileInputStream(path);

            result = FileUtils.readInteger(inputFile);

            inputFile.close();
        } catch (Exception e) {

        }

        return result;
    }

    public static void iterateOnDocumentFeatureSetListFile(String path, IDocumentFeatureSetLoaderCallback callback) {
        try {
            FileInputStream inputFile = new FileInputStream(path);

            int totalDocumentCount = FileUtils.readInteger(inputFile);
            for (int i = 0; i < totalDocumentCount; i++) {
                int length = FileUtils.readInteger(inputFile);
                byte[] data = FileUtils.readByteArray(inputFile, length);

                DocumentFeatureSet document = new DocumentFeatureSet();
                document.load(data);

                callback.processDocumentFeatureSet(document, i, totalDocumentCount);
            }

            inputFile.close();
        } catch (Exception e) {

        }
    }

    public static void mergeFiles(String path, ArrayList<String> fileList) {
        int total = sumOfCount(fileList);

        File file = new File(path);
        try {
            FileOutputStream fs = new FileOutputStream(file);
            writeInt(fs, total);

            for (String filePath : fileList) {
                ArrayList<DocumentFeatureSet> featuresList = loadListFromFile(filePath);
                for (DocumentFeatureSet featureSet : featuresList) {
                    byte[] data = featureSet.serialize();

                    writeInt(fs, data.length);
                    fs.write(data);
                }
            }

            fs.flush();
            fs.close();
        } catch (Exception e) {
//            Logger.error(e, "Unable to write file at path \"%s\".", path);
        }

    }

    private static int sumOfCount(ArrayList<String> fileList) {
        int result = 0;

        for (String path : fileList) {
            result += getDocumentFeatureSetCountInFile(path);
        }

        return result;
    }

    public Vector toVector() {
        Vector result = new Vector();

        for (String feature : features.keySet()) {
            result.addWeight(feature, features.get(feature));
        }

        result.identifier = String.format("year: %d; pmid: %s", documentYear, documentIdentifier);

        result.optimize();

        return result;
    }
}

class KeyValuePair {
    public String key;
    public double value;

    public KeyValuePair(String key, double value) {
        this.key = key;
        this.value = value;
    }
}
