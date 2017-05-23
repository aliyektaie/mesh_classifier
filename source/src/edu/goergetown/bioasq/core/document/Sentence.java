package edu.goergetown.bioasq.core.document;

import edu.goergetown.bioasq.utils.BinaryBuffer;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/11/2017.
 */
public class Sentence {
    public ArrayList<Term> tokens = new ArrayList<>();

    public void load(byte[] bytes) {
        BinaryBuffer buffer = new BinaryBuffer(bytes);

        int size = buffer.readShort();
        for (int i = 0; i < size; i++) {
            Term t = new Term();

            t.token = buffer.readString();
            t.partOfSpeech = buffer.readString();

            tokens.add(t);
        }
    }

    public byte[] serialize() {
        BinaryBuffer buffer = new BinaryBuffer(getSerializedLength());

        buffer.append((short)tokens.size());
        for (Term t : tokens) {
            buffer.append(t.token);
            buffer.append(t.partOfSpeech);
        }

        return buffer.getBuffer();
    }

    public int getSerializedLength() {
        int result = 2;

        for (Term t : tokens) {
            result += BinaryBuffer.getSerializedLength(t.token);
            result += BinaryBuffer.getSerializedLength(t.partOfSpeech);
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (Term t:tokens) {
            result.append(t.toString());
            result.append(" ");
        }

        return result.toString();
    }

    public String toString(boolean includePOS) {
        StringBuilder result = new StringBuilder();

        for (Term t:tokens) {
            if (includePOS) {
                result.append(t.toString());
            } else {
                result.append(t.token);
            }
            result.append(" ");
        }

        return result.toString();
    }
}
