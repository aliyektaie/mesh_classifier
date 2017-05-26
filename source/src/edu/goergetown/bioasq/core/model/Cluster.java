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

    public static void saveClusters(String path, ArrayList<Cluster> clusters) {
        int length = 4;
        length += 4 * clusters.size();
        int indexOffset = length;

        ArrayList<byte[]> data = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            byte[] d = clusters.get(i).serialize();
            length += BinaryBuffer.getSerializedLength(d);
            data.add(d);
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append(data.size());
        int lengthUpToHere = 0;
        for (int i = 0; i < data.size(); i++) {
            buffer.append(indexOffset + lengthUpToHere);
            lengthUpToHere += (4 + data.get(i).length);
        }

        for (int i = 0; i < data.size(); i++) {
            buffer.append(data.get(i));
        }

        FileUtils.writeBinary(path, buffer.getBuffer());
    }

    public static ArrayList<Cluster> loadClusters(String path) {
        ArrayList<Cluster> result = new ArrayList<>();
        BinaryBuffer buffer = new BinaryBuffer(FileUtils.readBinaryFile(path));
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            buffer.readInt();
        }

        for (int i = 0; i < count; i++) {
            Cluster cluster = new Cluster();
            cluster.load(buffer.readByteArray());

            result.add(cluster);
        }

        return result;
    }

    public byte[] serialize() {
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

        return buffer.getBuffer();
    }

    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);

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

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("edu.goergetown.bioasq.core.model.Cluster:");
        result.append("\r\n    Centroid: ");
        result.append(centroid.toString().replace("\r\n", "\r\n    "));
        result.append("\r\n    Vectors[").append(vectors.size()).append("]:");
        for (int i = 0; i < vectors.size(); i++) {
            Vector v = vectors.get(i);
            result.append("\r\n        [").append(i).append("] ");
            result.append(v.toString().replace("\r\n", "\r\n        "));
        }

        return result.toString();
    }

}
