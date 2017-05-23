package edu.goergetown.bioasq.core.document;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.FileUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Yektaie on 5/11/2017.
 */
public class Document {
    public String identifier = "";
    public Sentence title = null;
    public ArrayList<Sentence> text = new ArrayList<>();
    public Hashtable<String, String> metadata = new Hashtable<>();
    public ArrayList<String> categories = new ArrayList<>();

    private static MaxentTagger tagger = new MaxentTagger(Constants.STANFORD_POS_MODEL_FILE_PATH);

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("Identifier: " + identifier);
        result.append("\nTitle: \"" + title.toString(false) + "\"");
        result.append("\nText:");
        String del = "";
        for (Sentence s : text){
            result.append(del);
            del = "\n";
            result.append("\n    \"" + s.toString(false) + "\"");
            result.append("\n    \"" + s.toString() + "\"");
        }
        result.append("\nMeSH:");
        for (String mesh : categories) {
            result.append("\n    \"" + mesh + "\"");
        }
        result.append("\nMetadata:");
        for (String key : metadata.keySet()) {
            result.append("\n    " + key + ": " + metadata.get(key));
        }


        return result.toString();
    }

    public static Document fromBioAsq(BioAsqEntry entry) {
        Document result = new Document();

        result.identifier = entry.pmid;
        result.categories = entry.meshMajor;
        result.title = toSentence(entry.title);

        result.metadata.put("year", String.valueOf(entry.year));
        result.metadata.put("journal", entry.journal);

        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new StringReader(entry.abstractText)));
        for (List<HasWord> sentence : sentences) {
            List<TaggedWord> tSentence = tagger.tagSentence(sentence);

            Sentence s = new Sentence();
            for (TaggedWord word : tSentence) {
                Term t = new Term();
                t.token = word.word();
                t.partOfSpeech = word.tag();

                s.tokens.add(t);
            }

            result.text.add(s);
        }

        return result;
    }

    private static Sentence toSentence(String text) {
        Sentence s = new Sentence();

        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new StringReader(text)));
        for (List<HasWord> sentence : sentences) {
            List<TaggedWord> tSentence = tagger.tagSentence(sentence);

            for (TaggedWord word : tSentence) {
                Term t = new Term();
                t.token = word.word();
                t.partOfSpeech = word.tag();

                s.tokens.add(t);
            }
        }

        return s;
    }

    public void load(byte[] array) {
        BinaryBuffer buffer = new BinaryBuffer(array);

        identifier = buffer.readString();
        title = new Sentence();
        title.load(buffer.readByteArray());

        int size = buffer.readByte();
        for (int i = 0; i < size; i++) {
            Sentence s = new Sentence();
            s.load(buffer.readByteArray());

            text.add(s);
        }

        size = buffer.readByte();
        for (int i = 0; i < size; i++) {
            String key = buffer.readString();
            String value = buffer.readString();

            metadata.put(key, value);
        }

        size = buffer.readByte();
        for (int i = 0; i < size; i++) {
            categories.add(buffer.readString());
        }
    }

    public byte[] serialize() {
        BinaryBuffer buffer = new BinaryBuffer(getSerializedLength());

        buffer.append(identifier);
        buffer.append(title.serialize());

        buffer.append((byte)text.size());
        for (Sentence sentence : text) {
            buffer.append(sentence.serialize());
        }

        buffer.append((byte)metadata.size());
        for (String key : metadata.keySet()) {
            buffer.append(key);
            buffer.append(metadata.get(key));
        }

        buffer.append((byte)categories.size());
        for (String category : categories) {
            buffer.append(category);
        }

        return buffer.getBuffer();
    }

    public int getSerializedLength() {
        int result = 0;

        result += BinaryBuffer.getSerializedLength(identifier);
        result += 4;
        result += title.getSerializedLength();

        result += 1;
        result += (4 * text.size());
        for (Sentence sentence : text) {
            result += sentence.getSerializedLength();
        }

        result += 1;
        for (String key : metadata.keySet()) {
            result += BinaryBuffer.getSerializedLength(key);
            result += BinaryBuffer.getSerializedLength(metadata.get(key));
        }

        result += 1;
        for (String category : categories) {
            result += BinaryBuffer.getSerializedLength(category);
        }

        return result;
    }

    public static void saveList(String path, ArrayList<Document> documents) {
        File file = new File(path);
        try {
            FileOutputStream fs = new FileOutputStream(file);
            writeInt(fs, documents.size());

            for (Document document : documents) {
                byte[] data = document.serialize();

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

    public static ArrayList<Document> loadListFromFile(String path) {
        byte[] data = FileUtils.readBinaryFile(path);
        BinaryBuffer buffer = new BinaryBuffer(data);
        ArrayList<Document> result = new ArrayList<>();

        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            try {
                Document doc = new Document();
                doc.load(buffer.readByteArray());

                result.add(doc);
            } catch (Exception ex) {

            }
        }

        return result;
    }

    public static int getDocumentCountInFile(String path) {
        int result = 0;

        try {
            FileInputStream inputFile = new FileInputStream(path);

            result = FileUtils.readInteger(inputFile);

            inputFile.close();
        } catch (Exception e) {

        }

        return result;
    }

    public static void iterateOnDocumentListFile(String path, IDocumentLoaderCallback callback) {
        iterateOnDocumentListFile(path, callback, -1);
    }

    public static void iterateOnDocumentListFile(String path, IDocumentLoaderCallback callback, int maxCount) {
        try {
            FileInputStream inputFile = new FileInputStream(path);

            int totalDocumentCount = FileUtils.readInteger(inputFile);
            if (maxCount > 0) {
                totalDocumentCount = Math.min(totalDocumentCount, maxCount);
            }

            for (int i = 0; i < totalDocumentCount; i++) {
                int length = FileUtils.readInteger(inputFile);
                byte[] data = FileUtils.readByteArray(inputFile, length);

                Document document = new Document();
                document.load(data);

                callback.processDocument(document, i, totalDocumentCount);
            }

            inputFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
