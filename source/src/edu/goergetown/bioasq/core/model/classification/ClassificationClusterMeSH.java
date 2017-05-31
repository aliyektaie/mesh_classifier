package edu.goergetown.bioasq.core.model.classification;

import edu.goergetown.bioasq.utils.BinaryBuffer;

/**
 * Created by yektaie on 5/29/17.
 */
public class ClassificationClusterMeSH {
    public String mesh = "";
    public double weight = 0;

    public ClassificationClusterMeSH(String mesh, double weight) {
        this.mesh = mesh;
        this.weight = weight;
    }

    public ClassificationClusterMeSH() {

    }

    public byte[] serialize() {
        int length = getSerializedLength();

        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append(mesh);
        buffer.append((float) weight);

        return buffer.getBuffer();
    }

    public int getSerializedLength() {
        int length = BinaryBuffer.getSerializedLength(mesh);
        length += 4;
        return length;
    }

    public void load(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);

        this.mesh = buffer.readString();
        this.weight = buffer.readFloat();
    }
}
