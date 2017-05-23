package edu.goergetown.bioasq.core.model;

import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.FileUtils;
import java.util.ArrayList;

public class Cluster {
    public ArrayList<Vector> vectors = new ArrayList<>();
    public Vector centroid = new Vector();

    public void addVector(Vector vector, boolean updateCentroid) {
        synchronized (this) {
            vectors.add(vector);
            if (updateCentroid) {
                centroid = centroid.add(vector);
            }
        }
    }

    public void updateCentroid() {
        Vector result = new Vector();

        for (Vector vector : vectors) {
            result = result.add(vector);
        }

        result.optimize();

        centroid = result;
    }

    public int getVectorsSize() {
        return vectors.size();
    }

    public void clearVectors() {
        vectors.clear();
    }

    public Vector getCentroid() {
        return centroid;
    }

    public void save(String path) {
        ArrayList<byte[]> data = new ArrayList<>();
        byte[] centroidSerialized = centroid.serialize();

        int length = 4;
        length += BinaryBuffer.getSerializedLength(centroidSerialized);

        for (Vector vector : vectors) {
            byte[] s = vector.serialize();
            length += BinaryBuffer.getSerializedLength(s);
            data.add(s);
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append(centroidSerialized);

        buffer.append(vectors.size());
        for (byte[] vector : data) {
            buffer.append(vector);
        }

        FileUtils.writeBinary(path, buffer.getBuffer());
    }

    public void load(String path) {
        BinaryBuffer buffer = new BinaryBuffer(FileUtils.readBinaryFile(path));

        centroid = new Vector();
        centroid.load(buffer.readByteArray());
        centroid.optimize();

        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            Vector v = new Vector();
            v.load(buffer.readByteArray());
            v.optimize();

            vectors.add(v);
        }
    }
}
