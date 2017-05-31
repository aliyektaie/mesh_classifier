package edu.goergetown.bioasq.core.model.classification;

import edu.goergetown.bioasq.core.Guid;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.utils.BinaryBuffer;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/29/17.
 */
public class ClassificationCluster {
    public ArrayList<ClassificationClusterMeSH> meshes = new ArrayList<>();
    public Vector centroid = null;
    public Guid identifier = null;

    public byte[] serialize() {
        int length = getSerializedLength();
        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append(identifier.toString());

        buffer.append(meshes.size());
        for (ClassificationClusterMeSH m : meshes) {
            buffer.append(m.serialize());
        }

        buffer.append(centroid.serialize());

        return buffer.getBuffer();
    }

    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);

        identifier = new Guid(buffer.readString());
        int count = buffer.readInt();

        for (int i = 0; i < count; i++) {
            byte[] d = buffer.readByteArray();
            ClassificationClusterMeSH m = new ClassificationClusterMeSH();
            m.load(d);

            meshes.add(m);
        }

        centroid = new Vector();
        centroid.load(buffer.readByteArray());
    }

    public int getSerializedLength() {
        int result = 4;
        for (ClassificationClusterMeSH m : meshes) {
            result += m.getSerializedLength();
            result += 4;
        }

        result += BinaryBuffer.getSerializedLength(identifier.toString());
        result += 4;
        result += centroid.getSerializedLength();

        return result;
    }
}
