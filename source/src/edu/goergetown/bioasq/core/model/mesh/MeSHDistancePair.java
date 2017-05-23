package edu.goergetown.bioasq.core.model.mesh;

import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.utils.BinaryBuffer;

import java.util.Hashtable;

/**
 * Created by Yektaie on 5/22/2017.
 */
public class MeSHDistancePair {
    public double similarity = 0;
    public Vector vector1 = null;
    public Vector vector2 = null;

    public byte[] serialize() {
        int length = 4 + (2 + 4) + (2 + 4);
        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append((float)similarity);

        buffer.append(getYearFromVector(vector1));
        buffer.append(getIdFromVector(vector1));

        buffer.append(getYearFromVector(vector2));
        buffer.append(getIdFromVector(vector2));

        return buffer.getBuffer();
    }

    private int getIdFromVector(Vector vector) {
        String[] parts = vector.identifier.split(";");
        String p = parts[1].trim();
        p = p.substring(p.indexOf(":") + 2);

        return Integer.valueOf(p);
    }

    private short getYearFromVector(Vector vector) {
        String[] parts = vector.identifier.split(";");
        String p = parts[0].trim();
        p = p.substring(p.indexOf(":") + 2);

        return Short.valueOf(p);
    }

    public void load(byte[] data, Hashtable<String, Vector> vectors) {
        BinaryBuffer buffer = new BinaryBuffer(data);

        similarity = buffer.readFloat();

        int y1 = buffer.readShort();
        int i1 = buffer.readInt();

        String identifier1 = String.format("year: %d; pmid: %d", y1, i1);

        int y2 = buffer.readShort();
        int i2 = buffer.readInt();

        String identifier2 = String.format("year: %d; pmid: %d", y2, i2);

        vector1 = vectors.get(identifier1);
        vector2 = vectors.get(identifier2);
    }
}
