package edu.goergetown.bioasq.core.model.mesh;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/22/2017.
 */
public class MeSHVectorFile {
    public static String getFilePath() {
        return Constants.TEMP_FOLDER + "documents_mesh.bin";
    }

    public static String getIndexFilePath() {
        return Constants.TEMP_FOLDER + "documents_mesh.idx";
    }

    public static boolean exists() {
        return FileUtils.exists(getFilePath());
    }

    public static void save(ArrayList<Vector> result) {
        ArrayList<byte[]> data = new ArrayList<>();
        StringBuilder index = new StringBuilder();

        int length = 0;
        String del = "";

        for (int i = 0; i < result.size(); i++) {
            Vector vector = result.get(i);
            byte[] a = vector.serialize();

            index.append(del);
            index.append(String.format("%s -> %d:%d", vector.identifier, length, a.length));

            length += BinaryBuffer.getSerializedLength(a);
            data.add(a);
            del = "\r\n";
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        for (int i = 0; i < data.size(); i++) {
            byte[] d = data.get(i);
            buffer.append(d);
        }

        FileUtils.writeBinary(getFilePath(), buffer.getBuffer());
        FileUtils.writeText(getIndexFilePath(), index.toString());
    }

    public static void load(ArrayList<Vector> result) {
        BinaryBuffer buffer = new BinaryBuffer(FileUtils.readBinaryFile(getFilePath()));

        while (buffer.canRead()) {
            Vector vector = new Vector();
            vector.load(buffer.readByteArray());
            vector.optimize();

            result.add(vector);
        }
    }
}
